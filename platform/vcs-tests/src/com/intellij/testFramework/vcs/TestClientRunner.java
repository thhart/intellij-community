// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.testFramework.vcs;

import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class TestClientRunner {
  private static final Logger LOG = Logger.getInstance(TestClientRunner.class);
  private final boolean myTraceClient;
  private final File myClientBinaryPath;
  private final Map<String, String> myClientEnvironment;

  public TestClientRunner(boolean traceClient, File clientBinaryPath, @Nullable Map<String, String> clientEnvironment) {
    myTraceClient = traceClient;
    myClientBinaryPath = clientBinaryPath;
    myClientEnvironment = clientEnvironment;
  }

  public ProcessOutput runClient(@NotNull String exeName,
                                 @Nullable String stdin,
                                 final @Nullable File workingDir,
                                 String... commandLine) throws IOException {
    final List<String> arguments = new ArrayList<>();

    final File client = new File(myClientBinaryPath, SystemInfo.isWindows ? exeName + ".exe" : exeName);
    if (client.exists()) {
      arguments.add(client.toString());
    }
    else {
      // assume client is in path
      arguments.add(exeName);
    }
    Collections.addAll(arguments, commandLine);

    if (myTraceClient) {
      LOG.info("*** running:\n" + arguments);
      if (StringUtil.isNotEmpty(stdin)) {
        LOG.info("*** stdin:\n" + stdin);
      }
    }

    final ProcessBuilder builder = new ProcessBuilder().command(arguments);
    if (workingDir != null) {
      builder.directory(workingDir);
    }
    if (myClientEnvironment != null) {
      builder.environment().putAll(myClientEnvironment);
    }
    final Process clientProcess = builder.start();

    if (stdin != null) {
      try (OutputStream outputStream = clientProcess.getOutputStream()) {
        final byte[] bytes = stdin.getBytes(StandardCharsets.UTF_8);
        outputStream.write(bytes);
      }
    }

    final CapturingProcessHandler handler = new CapturingProcessHandler(clientProcess, CharsetToolkit.getDefaultSystemCharset(), StringUtil.join(arguments, " "));
    final ProcessOutput result = handler.runProcess(100*1000, false);
    if (myTraceClient || result.isTimeout()) {
      LOG.debug("*** result: " + result.getExitCode());
      final String out = result.getStdout().trim();
      if (!out.isEmpty()) {
        LOG.debug("*** output:\n" + out);
      }
      final String err = result.getStderr().trim();
      if (!err.isEmpty()) {
        LOG.debug("*** error:\n" + err);
      }
    }

    if (result.isTimeout()) {
      String processList = ProcessHandle.allProcesses().map(h -> h.pid() + ": " + h.info()).collect(Collectors.joining("\n"));
      handler.destroyProcess();
      throw new RuntimeException("Timeout waiting for VCS client to finish execution:\n" + processList);
    }

    return result;
  }
}
