// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.spellchecker.tokenizer;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.spellchecker.inspections.Splitter;
import org.jetbrains.annotations.NotNull;

public class TokenizerBase<T extends PsiElement> extends Tokenizer<T> {
  public static <T extends PsiElement> TokenizerBase<T> create(Splitter splitter) {
    return new TokenizerBase<>(splitter);
  }

  private final Splitter mySplitter;

  @Override
  public String toString() {
    return "TokenizerBase(splitter=" + mySplitter + ")";
  }

  public TokenizerBase(Splitter splitter) {
    mySplitter = splitter;
  }

  @Override
  public void tokenize(@NotNull T element, @NotNull TokenConsumer consumer) {
    if (element instanceof PsiLanguageInjectionHost
        && InjectedLanguageManager.getInstance(element.getProject()).getInjectedPsiFiles(element) != null) {
      return;
    }

    consumeToken(element, consumer, mySplitter);
  }

  public void consumeToken(@NotNull T element, @NotNull TokenConsumer consumer, @NotNull Splitter splitter) {
    consumer.consumeToken(element, splitter);
  }
}
