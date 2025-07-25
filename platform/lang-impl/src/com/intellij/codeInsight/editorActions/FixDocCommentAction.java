// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.codeInsight.editorActions;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.FileModificationService;
import com.intellij.codeInsight.documentation.DocCommentFixer;
import com.intellij.lang.*;
import com.intellij.lang.documentation.CodeDocumentationProvider;
import com.intellij.lang.documentation.CompositeDocumentationProvider;
import com.intellij.lang.documentation.DocumentationProvider;
import com.intellij.modcommand.ModPsiNavigator;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.DocCommentSettings;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.util.text.CharArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Creates documentation comment for the current context if it's not created yet (e.g. the caret is inside a method which
 * doesn't have a doc comment).
 * <p/>
 * Updates existing documentation comment if necessary if the one exists. E.g. we've changed method signature and want to remove all
 * outdated parameters and create stubs for the new ones.
 */
public final class FixDocCommentAction extends EditorAction {

  public static final @NotNull @NonNls String ACTION_ID = "FixDocComment";

  public FixDocCommentAction() {
    super(new MyHandler());
  }

  private static final class MyHandler extends EditorActionHandler {

    @Override
    public void doExecute(@NotNull Editor editor, @Nullable Caret caret, DataContext dataContext) {
      Project project = CommonDataKeys.PROJECT.getData(dataContext);
      if (project == null) {
        return;
      }

      PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataContext);
      if (psiFile == null) {
        return;
      }

      process(psiFile, editor, project, editor.getCaretModel().getOffset());
    }
  }

  private static void process(final @NotNull PsiFile file, final @NotNull Editor editor, final @NotNull Project project, int offset) {
    PsiElement elementAtOffset = file.findElementAt(offset);
    if (elementAtOffset == null || !FileModificationService.getInstance().preparePsiElementForWrite(elementAtOffset)) {
      return;
    }
    generateOrFixComment(elementAtOffset, project, editor);
  }

  /**
   * Generates a comment if it does not exist
   *
   * @param element     target element for which a comment should be generated
   * @param project     current project
   * @param navigator   navigator to use to set caret position
   */
  public static void generateComment(final @NotNull PsiElement element, final @NotNull Project project, final @NotNull ModPsiNavigator navigator) {
    Language language = element.getLanguage();
    final CodeDocumentationProvider docProvider = getDocumentationProvider(language);
    if (docProvider == null) {
      return;
    }

    final Pair<PsiElement, PsiComment> pair = docProvider.parseContext(element);
    if (pair == null || pair.second != null) {
      return;
    }

    Commenter c = LanguageCommenters.INSTANCE.forLanguage(language);
    if (!(c instanceof CodeDocumentationAwareCommenter commenter)) {
      return;
    }
    generateComment(pair.first, navigator, docProvider, commenter, project);
  }

  private static @Nullable CodeDocumentationProvider getDocumentationProvider(Language language) {
    final CodeDocumentationProvider docProvider;
    final DocumentationProvider langDocumentationProvider = LanguageDocumentation.INSTANCE.forLanguage(language);
    if (langDocumentationProvider instanceof CompositeDocumentationProvider provider) {
      docProvider = provider.getFirstCodeDocumentationProvider();
    }
    else if (langDocumentationProvider instanceof CodeDocumentationProvider provider) {
      docProvider = provider;
    }
    else {
      docProvider = null;
    }
    return docProvider;
  }

  /**
   * Generates comment if it's not exist or try to fix if exists
   *
   * @param element     target element for which a comment should be generated
   * @param project     current project
   * @param editor      target editor
   */
  public static void generateOrFixComment(final @NotNull PsiElement element, final @NotNull Project project, final @NotNull Editor editor) {
    Language language = element.getLanguage();
    final CodeDocumentationProvider docProvider = getDocumentationProvider(language);
    if (docProvider == null) {
      return;
    }

    final Pair<PsiElement, PsiComment> pair = docProvider.parseContext(element);
    if (pair == null) {
      return;
    }

    Commenter c = LanguageCommenters.INSTANCE.forLanguage(language);
    if (!(c instanceof CodeDocumentationAwareCommenter commenter)) {
      return;
    }
    final Runnable task;
    if (pair.second == null || pair.second.getTextRange().isEmpty()) {
      task = () -> generateComment(pair.first, EditorUtil.asPsiNavigator(editor), docProvider, commenter, project);
    }
    else {
      final DocCommentFixer fixer = DocCommentFixer.EXTENSION.forLanguage(language);
      if (fixer == null) {
        return;
      }
      else {
        task = () -> fixer.fixComment(project, editor, pair.second);
      }
    }
    if (!element.isPhysical()) {
      task.run();
    } else {
      final Runnable command = () -> ApplicationManager.getApplication().runWriteAction(task);
      CommandProcessor.getInstance().executeCommand(project, command, CodeInsightBundle.message("command.fix.documentation"), null);
    }
  }

  /**
   * Generates a comment if possible.
   * <p/>
   * It's assumed that this method {@link PsiDocumentManager#commitDocument(Document) syncs} all PSI-document
   * changes during the processing.
   *
   * @param anchor      target element for which a comment should be generated
   * @param navigator   navigator to use
   * @param commenter   commenter to use
   * @param project     current project
   */
  private static void generateComment(@NotNull PsiElement anchor,
                                      @NotNull ModPsiNavigator navigator,
                                      @NotNull CodeDocumentationProvider documentationProvider,
                                      @NotNull CodeDocumentationAwareCommenter commenter,
                                      @NotNull Project project) {
    Document document = anchor.getContainingFile().getFileDocument();
    int commentStartOffset = anchor.getTextRange().getStartOffset();
    int lineStartOffset = document.getLineStartOffset(document.getLineNumber(commentStartOffset));
    CharSequence charSequence = document.getCharsSequence();
    if (lineStartOffset > 0 && lineStartOffset < commentStartOffset) {
      // Example:
      //    void test1() {
      //    }
      //    void test2() {
      //       <offset>
      //    }
      // We want to insert the comment at the start of the line where 'test2()' is declared.
      int nonWhiteSpaceOffset = CharArrayUtil.shiftBackward(charSequence, lineStartOffset, commentStartOffset - 1, " \t") + 1;
      commentStartOffset = Math.max(nonWhiteSpaceOffset, lineStartOffset);
    }

    int commentBodyRelativeOffset = 0;
    int caretLineOffset = 0;
    StringBuilder buffer = new StringBuilder();
    String commentPrefix = commenter.getDocumentationCommentPrefix();
    if (commentPrefix != null) {
      buffer.append(commentPrefix).append("\n");
      caretLineOffset++;
      commentBodyRelativeOffset += commentPrefix.length() + 1;
    }

    String linePrefix = commenter.getDocumentationCommentLinePrefix();
    if (linePrefix != null) {
      buffer.append(linePrefix);
      commentBodyRelativeOffset += linePrefix.length();
    }
    buffer.append("\n");
    commentBodyRelativeOffset++;

    String commentSuffix = commenter.getDocumentationCommentSuffix();
    if (commentSuffix != null) {
      buffer.append(commentSuffix).append("\n");
    }

    if (buffer.length() <= 0) {
      return;
    }

    document.insertString(commentStartOffset, buffer);
    PsiDocumentManager docManager = PsiDocumentManager.getInstance(project);
    docManager.commitDocument(document);

    Pair<PsiElement, PsiComment> pair = documentationProvider.parseContext(anchor);
    if (pair == null || pair.second == null) {
      return;
    }

    int insertionOffset = commentStartOffset + commentBodyRelativeOffset;
    boolean inserted = documentationProvider.insertDocumentationContentStub(pair.second, document, insertionOffset);
    if (!inserted) {
      String stub = documentationProvider.generateDocumentationContentStub(pair.second);
      if (stub != null) {
        document.insertString(insertionOffset, stub);
        inserted = true;
      }
    }
    if (inserted) {
      docManager.commitDocument(document);
      pair = documentationProvider.parseContext(anchor);
    }

    navigator.moveCaretTo(commentStartOffset);
    navigator.select(TextRange.from(commentStartOffset, 0));

    if (pair == null || pair.second == null) {
      return;
    }

    int start = Math.min(calcStartReformatOffset(pair.first), calcStartReformatOffset(pair.second));
    int end = pair.second.getTextRange().getEndOffset();

    reformatCommentKeepingEmptyTags(anchor.getContainingFile(), project, start, end);
    int caretOffset = document.getLineEndOffset(document.getLineNumber(navigator.getCaretOffset()) + caretLineOffset);
    navigator.moveCaretTo(caretOffset);

    if (caretOffset > 0 && caretOffset <= document.getTextLength()) {
      char c = charSequence.charAt(caretOffset - 1);
      if (!StringUtil.isWhiteSpace(c)) {
        document.insertString(caretOffset, " ");
        navigator.moveCaretTo(caretOffset + 1);
      }
    }
  }

  private static void reformatCommentKeepingEmptyTags(@NotNull PsiFile file, @NotNull Project project, int start, int end) {
    CodeStyle.runWithLocalSettings(
      project,
      CodeStyle.getSettings(file),
      tempSettings -> {
        LanguageCodeStyleSettingsProvider langProvider =
          LanguageCodeStyleSettingsProvider.forLanguage(file.getLanguage());
        if (langProvider != null) {
          DocCommentSettings docCommentSettings = langProvider.getDocCommentSettings(tempSettings);
          docCommentSettings.setRemoveEmptyLines(true);
          docCommentSettings.setRemoveEmptyTags(false);
        }
        CodeStyleManager.getInstance(project).reformatText(file, start, end);
      });
  }

  private static int calcStartReformatOffset(@NotNull PsiElement element) {
    int result = element.getTextRange().getStartOffset();
    for (PsiElement e = element.getPrevSibling(); e != null; e = e.getPrevSibling()) {
      if (e instanceof PsiWhiteSpace) {
        result = e.getTextRange().getStartOffset();
      }
      else {
        break;
      }
    }
    return result;
  }
}
