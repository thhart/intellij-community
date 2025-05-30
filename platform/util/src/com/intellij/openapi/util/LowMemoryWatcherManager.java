// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ConcurrencyUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.concurrency.SequentialTaskExecutor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.NotificationListener;
import java.lang.management.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@ApiStatus.Internal
public final class LowMemoryWatcherManager {
  private static @NotNull Logger getLogger() {
    return Logger.getInstance(LowMemoryWatcherManager.class);
  }

  private static final long MEM_THRESHOLD = 5 /*MB*/ * 1024 * 1024;
  private static final long GC_TIME_THRESHOLD = 10_000; //20 seconds

  private final AtomicLong lastGcTime = new AtomicLong();

  private final ExecutorService myExecutorService;

  private Future<?> mySubmitted; // guarded by myJanitor
  private final Future<?> myMemoryPoolMXBeansFuture;
  private final Consumer<Boolean> myJanitor = new Consumer<Boolean>() {
    @Override
    public void accept(@NotNull Boolean afterGc) {
      // Clearing `mySubmitted` before all listeners are called, to avoid data races when a listener is added in the middle of execution
      // and is lost. This may, however, cause listeners to execute more than once (potentially even in parallel).
      synchronized (myJanitor) {
        mySubmitted = null;
      }
      LowMemoryWatcher.onLowMemorySignalReceived(afterGc);
    }
  };

  public LowMemoryWatcherManager(@NotNull ExecutorService backendExecutorService) {
    // whether LowMemoryWatcher runnables should be executed on the same thread that the low memory events come
    myExecutorService = Boolean.getBoolean("low.memory.watcher.sync") ?
      ConcurrencyUtil.newSameThreadExecutorService() :
      SequentialTaskExecutor.createSequentialApplicationPoolExecutor("LowMemoryWatcherManager", backendExecutorService);

    myMemoryPoolMXBeansFuture = initializeMXBeanListenersLater(backendExecutorService);
    lastGcTime.set(getMajorGcTime());
  }

  private static long getMajorGcTime() {
    for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
      if (gc.getName().toLowerCase().contains("g1 old generation")) {
        return gc.getCollectionTime();
      }
    }
    return 0;
  }

  private @NotNull Future<?> initializeMXBeanListenersLater(@NotNull ExecutorService backendExecutorService) {
    // do it in the other thread to get it out of the way during startup
    return backendExecutorService.submit(new Runnable() {
      @Override
      public void run() {
        try {
          for (MemoryPoolMXBean bean : ManagementFactory.getMemoryPoolMXBeans()) {
            if (bean.getType() == MemoryType.HEAP && bean.isCollectionUsageThresholdSupported() && bean.isUsageThresholdSupported()) {
              long max = bean.getUsage().getMax();
              long threshold = Math.min((long)(max * getOccupiedMemoryThreshold()), max - MEM_THRESHOLD);
              if (threshold > 0) {
                bean.setUsageThreshold(threshold);
                bean.setCollectionUsageThreshold(threshold);
              }
            }
          }
          ((NotificationEmitter)ManagementFactory.getMemoryMXBean()).addNotificationListener(myLowMemoryListener, null, null);
        }
        catch (Throwable e) {
          // should not happen normally
          getLogger().info("Errors initializing LowMemoryWatcher: ", e);
        }
      }

      @Override
      public String toString() {
        return "initializeMXBeanListeners runnable";
      }
    });
  }

  private static class GcTracker {
    private static final long WINDOW_SIZE_MS = 60_000; // 1 minute
    private final Queue<GcPeriod> gcPeriods = new LinkedList<>();

    private static class GcPeriod {
      final long timestamp;
      final long gcTime;

      GcPeriod(long timestamp, long gcTime) {
        this.timestamp = timestamp;
        this.gcTime = gcTime;
      }
    }

    public synchronized long trackGcAndGetRecentTime(long currentGcTime, long previousGcTimeValue) {
      long currentTime = System.currentTimeMillis();

      if (currentGcTime > previousGcTimeValue) {
        gcPeriods.offer(new GcPeriod(currentTime, currentGcTime - previousGcTimeValue));
      }

      while (!gcPeriods.isEmpty() && gcPeriods.peek().timestamp < currentTime - WINDOW_SIZE_MS) {
        gcPeriods.poll();
      }

      return gcPeriods.stream()
        .mapToLong(period -> period.gcTime)
        .sum();
    }
  }

  private final GcTracker gcTracker = new GcTracker();

  private final NotificationListener myLowMemoryListener = new NotificationListener() {
    @Override
    public void handleNotification(Notification notification, Object __) {
      if (LowMemoryWatcher.notificationsSuppressed()) return;
      boolean memoryThreshold = MemoryNotificationInfo.MEMORY_THRESHOLD_EXCEEDED.equals(notification.getType());
      boolean memoryCollectionThreshold = MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED.equals(notification.getType());

      if (memoryThreshold || memoryCollectionThreshold) {
        long currentGcTime = getMajorGcTime();
        long previousGcTimeValue = lastGcTime.getAndSet(currentGcTime);
        long recentGcTime = gcTracker.trackGcAndGetRecentTime(currentGcTime, previousGcTimeValue);

        synchronized (myJanitor) {
          if (mySubmitted == null) {
            mySubmitted = myExecutorService.submit(() -> myJanitor.accept(recentGcTime > GC_TIME_THRESHOLD));
            // maybe it's executed too fast or even synchronously
            if (mySubmitted.isDone()) {
              mySubmitted = null;
            }
          }
        }
      }
    }
  };

  private static float getOccupiedMemoryThreshold() {
    return SystemProperties.getFloatProperty("low.memory.watcher.notification.threshold", 0.95f);
  }

  public void shutdown() {
    try {
      myMemoryPoolMXBeansFuture.get();
      ((NotificationEmitter)ManagementFactory.getMemoryMXBean()).removeNotificationListener(myLowMemoryListener);
    }
    catch (Exception e) {
      getLogger().error(e);
    }
    synchronized (myJanitor) {
      if (mySubmitted != null) {
        mySubmitted.cancel(false);
        mySubmitted = null;
      }
    }

    LowMemoryWatcher.stopAll();
  }

  @TestOnly
  public void waitForInitComplete(int timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    myMemoryPoolMXBeansFuture.get(timeout, unit);
  }
}
