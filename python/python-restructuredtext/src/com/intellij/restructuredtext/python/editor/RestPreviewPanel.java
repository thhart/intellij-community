// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.restructuredtext.python.editor;

import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Every reStructuredText editor preview panel must implement this interface.
 */
public interface RestPreviewPanel extends Disposable {

  void setHtml(@NotNull String html);

  void render();

  /**
   * Returns the component that actually displays the rendered HTML and shown in the interface.
   */
  @NotNull JComponent getComponent();
}
