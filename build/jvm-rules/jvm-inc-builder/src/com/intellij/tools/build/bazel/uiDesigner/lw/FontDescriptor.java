// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.tools.build.bazel.uiDesigner.lw;

import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.Locale;
import java.util.Objects;

public final class FontDescriptor {
  private final String myFontName;
  private final int myFontSize;
  private final int myFontStyle;
  private final String mySwingFont;

  private FontDescriptor() {
    myFontName = null;
    myFontSize = 0;
    myFontStyle = 0;
    mySwingFont = null;
  }
  
  private FontDescriptor(String swingFont) {
    myFontName = null;
    myFontSize = 0;
    myFontStyle = 0;
    mySwingFont = swingFont;
  }

  public FontDescriptor(final String fontName, final int fontStyle, final int fontSize) {
    myFontName = fontName;
    myFontSize = fontSize;
    myFontStyle = fontStyle;
    mySwingFont = null;
  }

  public boolean isFixedFont() {
    return mySwingFont == null;
  }

  public static FontDescriptor fromSwingFont(String swingFont) {
    return new FontDescriptor(swingFont);
  }

  public String getFontName() {
    return myFontName;
  }

  public int getFontSize() {
    return myFontSize;
  }

  public int getFontStyle() {
    return myFontStyle;
  }

  public Font getFont(Font defaultFont) {
    if (myFontName == null && defaultFont == null) {
      return null;
    }
    String name = myFontName;
    if (name == null || name.isEmpty()) {
      if (defaultFont == null) {
        return null;
      }
      name = defaultFont.getName();
    }
    else {
      if (!isValidFontName()) {
        if (defaultFont == null) {
          return null;
        }
        name = defaultFont.getName();
      }
    }

    return getFontWithFallback(
      new Font(name, myFontStyle >= 0 ? myFontStyle : defaultFont.getStyle(), myFontSize >= 0 ? myFontSize : defaultFont.getSize()));
  }

  private static Font getFontWithFallback(Font font) {
    boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
    Font fontWithFallback = isMac
                            ? new Font(font.getFamily(), font.getStyle(), font.getSize())
                            : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
    return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
  }

  public String getSwingFont() {
    return mySwingFont;
  }

  public Font getResolvedFont(Font defaultFont) {
    if (mySwingFont != null) {
      Font result = UIManager.getFont(mySwingFont);
      return result == null ? defaultFont : result;
    }
    return getFont(defaultFont);
  }

  private boolean isValidFontName() {
    Font font = new Font(myFontName, Font.PLAIN, 10);
    return font.canDisplay('a') && font.canDisplay('1');
  }

  public boolean isValid() {
    if (mySwingFont == null) {
      return myFontName == null || isValidFontName();
    }
    return UIManager.getFont(mySwingFont) != null;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof FontDescriptor)) {
      return false;
    }
    FontDescriptor rhs = (FontDescriptor) obj;
    if (mySwingFont != null) {
      return mySwingFont.equals(rhs.mySwingFont);
    }
    else {
      if (myFontName == null && rhs.myFontName != null) return false;
      if (myFontName != null && rhs.myFontName == null) return false;
      if (myFontName != null && !myFontName.equals(rhs.myFontName)) return false;
      return myFontSize == rhs.myFontSize && myFontStyle == rhs.myFontStyle;
    }
  }

  @Override
  public int hashCode() {
    return mySwingFont != null? mySwingFont.hashCode() : Objects.hash(myFontName, myFontSize, myFontStyle);
  }
}
