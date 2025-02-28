// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.siyeh.ig.fixes;

import com.intellij.codeInspection.CommonQuickFixBundle;
import com.intellij.modcommand.ModPsiUpdater;
import com.intellij.modcommand.PsiUpdateModCommandQuickFix;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.util.PsiTreeUtil;
import com.siyeh.ig.PsiReplacementUtil;
import com.siyeh.ig.psiutils.*;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Bas Leijdekkers
 */
public final class EqualsToEqualityFix extends PsiUpdateModCommandQuickFix {

  private final boolean myNegated;

  private EqualsToEqualityFix(boolean negated) {
    myNegated = negated;
  }

  public static @Nullable EqualsToEqualityFix buildFix(PsiMethodCallExpression expressionToFix, boolean negated) {
    if (ExpressionUtils.isVoidContext(expressionToFix)) {
      // replacing top level equals() call will produce red code
      return null;
    }
    return new EqualsToEqualityFix(negated);
  }

  @Override
  public @Nls @NotNull String getFamilyName() {
    return myNegated
           ? CommonQuickFixBundle.message("fix.replace.x.with.y", "!equals()", "!=")
           : CommonQuickFixBundle.message("fix.replace.x.with.y", "equals()", "==");
  }

  @Override
  protected void applyFix(@NotNull Project project, @NotNull PsiElement element, @NotNull ModPsiUpdater updater) {
    if (element instanceof PsiExpression expr && BoolUtils.isNegation(expr)) {
      element = BoolUtils.getNegated(expr);
    }
    final PsiMethodCallExpression call = PsiTreeUtil.getParentOfType(element, PsiMethodCallExpression.class, false);
    EqualityCheck check = EqualityCheck.from(call);
    if (check == null) return;
    PsiExpression lhs = check.getLeft();
    PsiExpression rhs = check.getRight();
    final PsiElement parent = ParenthesesUtils.getParentSkipParentheses(call);
    final CommentTracker commentTracker = new CommentTracker();
    final String lhsText = commentTracker.text(lhs, ParenthesesUtils.EQUALITY_PRECEDENCE);
    final String rhsText = commentTracker.text(rhs, ParenthesesUtils.EQUALITY_PRECEDENCE);
    if (parent instanceof PsiExpression && BoolUtils.isNegation((PsiExpression)parent)) {
      PsiReplacementUtil.replaceExpression((PsiExpression)parent, lhsText + "!=" + rhsText, commentTracker);
    }
    else {
      PsiReplacementUtil.replaceExpression(call, lhsText + "==" + rhsText, commentTracker);
    }
  }
}
