// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.options;

import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.HtmlChunk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Thrown to indicate that a configurable component cannot {@link UnnamedConfigurable#apply() apply} entered values.
 */
public class ConfigurationException extends Exception {
  private @NlsContexts.DialogTitle String myTitle = getDefaultTitle();
  private ConfigurationQuickFix myQuickFix;
  private Configurable myOriginator;
  private boolean myIsHtmlMessage;

  /**
   * @param message the detail message describing the problem
   */
  public ConfigurationException(@NlsContexts.DialogMessage String message) {
    super(message);
  }

  /**
   * @param message the detailed message describing the problem
   * @param title   the title describing the problem in short
   */
  public ConfigurationException(@NlsContexts.DialogMessage String message,
                                @NlsContexts.DialogTitle String title) {
    super(message);
    myTitle = title;
  }

  public ConfigurationException(@NlsContexts.DialogMessage String message,
                                Throwable cause,
                                @NlsContexts.DialogTitle String title) {
    super(message, cause);
    myTitle = title;
  }

  /**
   * Sets the flag informing that this exception message is HTML, rather than the plain text 
   * 
   * @return this exception
   */
  public @NotNull ConfigurationException withHtmlMessage() {
    myIsHtmlMessage = true;
    return this;
  }

  /**
   * @return the exception message. 
   * 
   * @deprecated It can be either plain text, or HTML. Use {@link #getMessageHtml()} to get the correct HTML message always.
   */
  @Override
  @Deprecated
  public @NlsContexts.DialogMessage String getMessage() {
    //noinspection HardCodedStringLiteral
    return super.getMessage();
  }

  /**
   * @return HTML chunk representing the message.
   */
  public @NotNull HtmlChunk getMessageHtml() {
    String message = getMessage();
    return message == null ? HtmlChunk.empty() : myIsHtmlMessage ? HtmlChunk.raw(message) : HtmlChunk.text(message);
  }

  /**
   * @return the title describing the problem in short
   */
  public @NlsContexts.DialogTitle String getTitle() {
    return myTitle;
  }

  /**
   * @param quickFix a runnable task that can fix the problem somehow
   */
  public void setQuickFix(@Nullable Runnable quickFix) {
    myQuickFix = quickFix == null ? null : dataContext -> quickFix.run();
  }

  public void setQuickFix(@Nullable ConfigurationQuickFix quickFix) {
    myQuickFix = quickFix;
  }

  public @Nullable ConfigurationQuickFix getConfigurationQuickFix() {
    return myQuickFix;
  }

  public @Nullable Configurable getOriginator() {
    return myOriginator;
  }

  public void setOriginator(@Nullable Configurable originator) {
    myOriginator = originator;
  }

  /**
   * @return whether this error should be shown when index isn't complete. Override and return false for errors that
   * might be caused by inability to find some PSI due to index absence.
   */
  public boolean shouldShowInDumbMode() {
    return true;
  }

  public static @NlsContexts.DialogTitle String getDefaultTitle() {
    return OptionsBundle.message("cannot.save.settings.default.dialog.title");
  }
}
