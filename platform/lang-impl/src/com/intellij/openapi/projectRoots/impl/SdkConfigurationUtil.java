// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.projectRoots.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.*;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.DiskQueryRelay;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Consumer;
import com.intellij.util.NullableConsumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.text.UniqueNameGenerator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.ide.PooledThreadExecutor;

import java.awt.*;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;


public final class SdkConfigurationUtil {
  private static final Logger LOG = Logger.getInstance(SdkConfigurationUtil.class);

  private SdkConfigurationUtil() { }

  public static void createSdk(final @Nullable Project project,
                               Sdk @NotNull [] existingSdks,
                               @NotNull NullableConsumer<? super Sdk> onSdkCreatedCallBack,
                               final boolean createIfExists,
                               SdkType @NotNull ... sdkTypes) {
    if (sdkTypes.length == 0) {
      onSdkCreatedCallBack.consume(null);
      return;
    }

    FileChooserDescriptor descriptor = createCompositeDescriptor(sdkTypes);
    VirtualFile suggestedDir = getSuggestedSdkRoot(sdkTypes[0]);
    FileChooser.chooseFiles(descriptor, project, suggestedDir, new FileChooser.FileChooserConsumer() {
      @Override
      public void consume(List<VirtualFile> selectedFiles) {
        for (SdkType sdkType : sdkTypes) {
          final String path = selectedFiles.get(0).getPath();
          if (sdkType.isValidSdkHome(path)) {
            Sdk newSdk = null;
            if (!createIfExists) {
              for (Sdk sdk : existingSdks) {
                if (path.equals(sdk.getHomePath())) {
                  newSdk = sdk;
                  break;
                }
              }
            }
            if (newSdk == null) {
              newSdk = setupSdk(existingSdks, selectedFiles.get(0), sdkType, false, null, null);
            }
            onSdkCreatedCallBack.consume(newSdk);
            return;
          }
        }
        onSdkCreatedCallBack.consume(null);
      }

      @Override
      public void cancelled() {
        onSdkCreatedCallBack.consume(null);
      }
    });
  }

  public static void createSdk(final @Nullable Project project,
                               Sdk @NotNull [] existingSdks,
                               @NotNull NullableConsumer<? super Sdk> onSdkCreatedCallBack,
                               SdkType @NotNull ... sdkTypes) {
    createSdk(project, existingSdks, onSdkCreatedCallBack, true, sdkTypes);
  }

  private static @NotNull FileChooserDescriptor createCompositeDescriptor(SdkType @NotNull ... sdkTypes) {
    return new FileChooserDescriptor(sdkTypes[0].getHomeChooserDescriptor()) {
      @Override
      public void validateSelectedFiles(final VirtualFile @NotNull [] files) throws Exception {
        if (files.length > 0) {
          for (SdkType type : sdkTypes) {
            if (type.isValidSdkHome(files[0].getPath())) {
              return;
            }
          }
        }
        String key =
          files.length > 0 && files[0].isDirectory() ? "sdk.configure.home.invalid.error" : "sdk.configure.home.file.invalid.error";
        throw new Exception(ProjectBundle.message(key, sdkTypes[0].getPresentableName()));
      }
    };
  }

  public static void addSdk(final @NotNull Sdk sdk) {
    ApplicationManager.getApplication().runWriteAction(() -> ProjectJdkTable.getInstance().addJdk(sdk));
  }

  public static void removeSdk(@NotNull Sdk sdk) {
    ApplicationManager.getApplication().runWriteAction(() -> ProjectJdkTable.getInstance().removeJdk(sdk));
  }

  /**
   * Same as {@link #setupSdk(Sdk[], VirtualFile, SdkType, boolean, SdkAdditionalData, String)}
   * but doesn't catch exceptions
   */
  @ApiStatus.Internal
  public static @NotNull Sdk setupSdk(@NotNull Sdk @NotNull []allSdks,
                             @NotNull VirtualFile homeDir,
                             @NotNull SdkType sdkType,
                             final @Nullable SdkAdditionalData additionalData,
                             final @Nullable String customSdkSuggestedName) {
    Sdk sdk = createSdk(Arrays.asList(allSdks), homeDir, sdkType, additionalData, customSdkSuggestedName);
    sdkType.setupSdkPaths(sdk);
    return sdk;
  }

  /**
   * Creates SDK, catches any error, logs it, and shows error if not `silent`.
   * @see #setupSdk(Sdk[], VirtualFile, SdkType, SdkAdditionalData, String)
   */
  public static @Nullable Sdk setupSdk(Sdk @NotNull [] allSdks,
                             @NotNull VirtualFile homeDir,
                             @NotNull SdkType sdkType,
                             final boolean silent,
                             final @Nullable SdkAdditionalData additionalData,
                             final @Nullable String customSdkSuggestedName) {
    Sdk sdk = null;
    try {
      sdk = setupSdk(allSdks, homeDir, sdkType, additionalData, customSdkSuggestedName);
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable e) {
      LOG.warn("Error creating or configuring sdk: homeDir=[" + homeDir + "]; " +
               "sdkType=[" + sdkType + "]; " +
               "additionalData=[" + additionalData + "]; " +
               "customSdkSuggestedName=[" + customSdkSuggestedName + "]; " +
               "sdk=[" + sdk + "]", e);
      if (!silent) {
        ApplicationManager.getApplication().invokeLater(
          () ->
            Messages.showErrorDialog(
              ProjectBundle.message(
                "dialog.message.error.configuring.sdk.0.please.make.sure.that.1.is.a.valid.home.path.for.this.sdk.type",
                e.getMessage(),
                FileUtil.toSystemDependentName(homeDir.getPath())
              ),
              ProjectBundle.message("dialog.title.error.configuring.sdk")
            )
        );
      }
      return null;
    }
    return sdk;
  }

  public static @NotNull Sdk createSdk(@NotNull Collection<? extends Sdk> allSdks,
                                       @NotNull VirtualFile homeDir,
                                       @NotNull SdkType sdkType,
                                       @Nullable SdkAdditionalData additionalData,
                                       @Nullable String customSdkSuggestedName) {
    return createSdk(allSdks, sdkType.sdkPath(homeDir), sdkType, additionalData, customSdkSuggestedName);
  }

  public static @NotNull Sdk createSdk(@NotNull Collection<? extends Sdk> allSdks,
                                       @NotNull String homePath,
                                       @NotNull SdkType sdkType,
                                       @Nullable SdkAdditionalData additionalData,
                                       @Nullable String customSdkSuggestedName) {
    final String sdkName = customSdkSuggestedName == null
                           ? createUniqueSdkName(sdkType, homePath, allSdks)
                           : createUniqueSdkName(customSdkSuggestedName, allSdks);

    Sdk sdk = ProjectJdkTable.getInstance().createSdk(sdkName, sdkType);
    SdkModificator sdkModificator = sdk.getSdkModificator();
    if (additionalData != null) {
      // additional initialization.
      // E.g. some ruby sdks must be initialized before
      // setupSdkPaths() method invocation
      sdkModificator.setSdkAdditionalData(additionalData);
    }
    if (sdkModificator.getVersionString() == null && !homePath.isEmpty()) {
      sdkModificator.setVersionString(sdkType.getVersionString(homePath));
    }
    sdkModificator.setHomePath(homePath);

    Application application = ApplicationManager.getApplication();
    Runnable runnable = () -> sdkModificator.commitChanges();
    if (application.isDispatchThread()) {
      application.runWriteAction(runnable);
    }
    else {
      application.invokeAndWait(() -> application.runWriteAction(runnable));
    }
    return sdk;
  }

  public static void setDirectoryProjectSdk(final @NotNull Project project, final @Nullable Sdk sdk) {
    ApplicationManager.getApplication().runWriteAction(() -> {
      ProjectRootManager.getInstance(project).setProjectSdk(sdk);
      final Module[] modules = ModuleManager.getInstance(project).getModules();
      if (modules.length > 0) {
        ModuleRootModificationUtil.setSdkInherited(modules[0]);
      }
    });
  }

  public static void configureDirectoryProjectSdk(@NotNull Project project,
                                                  @Nullable Comparator<? super Sdk> preferredSdkComparator,
                                                  SdkType @NotNull ... sdkTypes) {
    Sdk existingSdk = ProjectRootManager.getInstance(project).getProjectSdk();
    if (existingSdk != null && ArrayUtil.contains(existingSdk.getSdkType(), sdkTypes)) {
      return;
    }

    Sdk sdk = findOrCreateSdk(preferredSdkComparator, sdkTypes);
    if (sdk != null) {
      setDirectoryProjectSdk(project, sdk);
    }
  }

  public static @Nullable Sdk findOrCreateSdk(@Nullable Comparator<? super Sdk> comparator, SdkType @NotNull ... sdkTypes) {
    final Project defaultProject = ProjectManager.getInstance().getDefaultProject();
    final Sdk sdk = ProjectRootManager.getInstance(defaultProject).getProjectSdk();
    if (sdk != null && ArrayUtil.contains(sdk.getSdkType(), sdkTypes)) {
      return sdk;
    }
    for (SdkType type : sdkTypes) {
      List<Sdk> sdks = ProjectJdkTable.getInstance().getSdksOfType(type);
      if (!sdks.isEmpty()) {
        if (comparator != null) {
          sdks = ContainerUtil.sorted(sdks, comparator);
        }
        return sdks.get(0);
      }
    }
    for (SdkType sdkType : sdkTypes) {
      for (String suggestedHomePath : sdkType.suggestHomePaths()) {
        if (sdkType.isValidSdkHome(suggestedHomePath)) {
          Sdk an_sdk = createAndAddSDK(suggestedHomePath, sdkType);
          if (an_sdk != null) return an_sdk;
        }
      }
    }
    return null;
  }

  /**
   * Tries to create an SDK identified by path; if successful, add the SDK to the global SDK table.
   * <p>
   * Must be called from the EDT (because it uses {@link WriteAction#compute} under the hood).
   *
   * @param path identifies the SDK
   * @return newly created SDK, or null.
   */
  public static @Nullable Sdk createAndAddSDK(@NotNull String path, @NotNull SdkType sdkType) {
    VirtualFile sdkHome = WriteAction.compute(() -> {
      return LocalFileSystem.getInstance().refreshAndFindFileByPath(FileUtil.toSystemIndependentName(path));
    });
    if (sdkHome != null) {
      final Sdk newSdk = setupSdk(ProjectJdkTable.getInstance().getAllJdks(), sdkHome, sdkType, true, null, null);
      if (newSdk != null) {
        addSdk(newSdk);
      }
      return newSdk;
    }
    return null;
  }

  /// Tries to create an SDK identified by path; if successful, add the SDK to the global SDK table.
  /// Contrary to [#createAndAddSDK(String, SdkType)], SDK paths are not setup.
  /// @param path identifies the SDK
  /// @return newly created incomplete SDK, or null.
  public static @Nullable Sdk createIncompleteSDK(@NotNull String path, @NotNull SdkType sdkType) {
    VirtualFile sdkHome = WriteAction.compute(() -> {
      return LocalFileSystem.getInstance().refreshAndFindFileByPath(FileUtil.toSystemIndependentName(path));
    });
    if (sdkHome == null) return null;

    Sdk newSdk = createSdk(Arrays.asList(ProjectJdkTable.getInstance().getAllJdks()), sdkHome, sdkType, null, null);
    addSdk(newSdk);

    return newSdk;
  }

  public static @NotNull String createUniqueSdkName(@NotNull SdkType type, @NotNull String home, final Collection<? extends Sdk> sdks) {
    return createUniqueSdkName(type.suggestSdkName(null, home), sdks);
  }

  public static @NotNull String createUniqueSdkName(@NotNull String suggestedName, @NotNull Collection<? extends Sdk> sdks) {
    Set<String> nameList = sdks.stream().map(jdk -> jdk.getName()).collect(Collectors.toSet());

    return UniqueNameGenerator.generateUniqueName(suggestedName, "", "", " (", ")", o -> !nameList.contains(o));
  }

  /**
   * @deprecated Please use {@link SdkConfigurationUtil#selectSdkHome(SdkType, Component, Path, Consumer)}
   */
  @Deprecated
  public static void selectSdkHome(final @NotNull SdkType sdkType, final @NotNull Consumer<? super String> consumer) {
    selectSdkHome(sdkType, null, Path.of(System.getProperty("user.home")), consumer);
  }

  public static boolean selectSdkHomeForTests(@NotNull SdkType sdkType, @NotNull Consumer<? super String> consumer) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      Sdk sdk = ProjectJdkTable.getInstance().findMostRecentSdkOfType(sdkType);
      if (sdk == null) throw new RuntimeException("No SDK of type " + sdkType + " found");
      consumer.consume(sdk.getHomePath());
      return true;
    }
    return false;
  }

  public static void selectSdkHome(final @NotNull SdkType sdkType,
                                   @Nullable Component component,
                                   @NotNull Path path,
                                   final @NotNull Consumer<? super String> consumer) {
    if (selectSdkHomeForTests(sdkType, consumer)) return;

    final FileChooserDescriptor descriptor = sdkType.getHomeChooserDescriptor();

    Future<VirtualFile> sdkRootFuture = PooledThreadExecutor.INSTANCE.submit(() -> getSuggestedSdkRoot(sdkType, path));
    VirtualFile suggestedSdkRoot = null;
    try {
      suggestedSdkRoot = sdkRootFuture.get(200, TimeUnit.MILLISECONDS);
    }
    catch (InterruptedException | ExecutionException | TimeoutException ignored) {
    }

    // passing project instance here seems to be the right idea, but it would make the dialog
    // selecting the last opened project path, instead of the suggested detected JDK home (one of many).
    // The behaviour may also depend on the FileChooser implementations which does not reuse that code
    FileChooser.chooseFiles(descriptor, null, component, suggestedSdkRoot, chosen -> {
      final String chosenPath = chosen.get(0).getPath();
      final String adjustedPath = sdkType.adjustSelectedSdkHome(chosenPath);
      AtomicBoolean isAdjustedPathValid = new AtomicBoolean(false);
      ProgressManager.getInstance().runProcessWithProgressSynchronously(
        () -> isAdjustedPathValid.set(DiskQueryRelay.compute(() -> sdkType.isValidSdkHome(adjustedPath))),
        ProjectBundle.message("progress.title.checking.sdk.home"), true, null
      );
      consumer.consume(isAdjustedPathValid.get() ? adjustedPath : chosenPath);
    });
  }

  /**
   * @deprecated Please use {@link SdkConfigurationUtil#getSuggestedSdkRoot(SdkType, Path)}
   */
  @Deprecated
  public static @Nullable VirtualFile getSuggestedSdkRoot(@NotNull SdkType sdkType) {
    return doGetSuggestedSdkRoot(sdkType.suggestHomePath());
  }

  public static @Nullable VirtualFile getSuggestedSdkRoot(@NotNull SdkType sdkType, @NotNull Path path) {
    return doGetSuggestedSdkRoot(sdkType.suggestHomePath(path));
  }

  private static @Nullable VirtualFile doGetSuggestedSdkRoot(@Nullable String homePath) {
    return homePath == null ? null : LocalFileSystem.getInstance().findFileByPath(homePath);
  }

  public static @NotNull List<String> filterExistingPaths(@NotNull SdkType sdkType, Collection<String> sdkHomes, final Sdk[] sdks) {
    List<String> result = new ArrayList<>();
    for (String sdkHome : sdkHomes) {
      if (findByPath(sdkType, sdks, sdkHome) == null) {
        result.add(sdkHome);
      }
    }
    return result;
  }

  public static @Nullable Sdk findByPath(@NotNull SdkType sdkType, Sdk @NotNull [] sdks, @NotNull String sdkHome) {
    for (Sdk sdk : sdks) {
      final String path = sdk.getHomePath();
      if (sdk.getSdkType() == sdkType && path != null &&
          FileUtil.pathsEqual(FileUtil.toSystemIndependentName(path), FileUtil.toSystemIndependentName(sdkHome))) {
        return sdk;
      }
    }
    return null;
  }
}
