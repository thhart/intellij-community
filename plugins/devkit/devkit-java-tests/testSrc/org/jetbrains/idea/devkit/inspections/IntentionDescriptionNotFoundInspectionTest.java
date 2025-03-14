// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.devkit.inspections;

import com.intellij.codeInsight.daemon.impl.analysis.HighlightFixUtil;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionBean;
import com.intellij.java.codeserver.highlighting.errors.JavaCompilationError;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.java.LanguageLevel;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.TestDataPath;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.devkit.DevkitJavaTestsUtil;

@TestDataPath("$CONTENT_ROOT/testData/inspections/intentionDescription")
public class IntentionDescriptionNotFoundInspectionTest extends JavaCodeInsightFixtureTestCase {

  @Override
  protected String getBasePath() {
    return DevkitJavaTestsUtil.TESTDATA_PATH + "inspections/intentionDescription";
  }

  @Override
  protected void tuneFixture(JavaModuleFixtureBuilder moduleBuilder) {
    moduleBuilder.setLanguageLevel(LanguageLevel.JDK_17);
    moduleBuilder.addJdk(IdeaTestUtil.getMockJdk18Path().getPath());
    moduleBuilder.addLibrary("core", PathUtil.getJarPathForClass(Project.class));
    moduleBuilder.addLibrary("editor", PathUtil.getJarPathForClass(Editor.class));
    moduleBuilder.addLibrary("analysis-api", PathUtil.getJarPathForClass(IntentionActionBean.class));
    moduleBuilder.addLibrary("platform-rt", PathUtil.getJarPathForClass(IncorrectOperationException.class));
    moduleBuilder.addLibrary("platform-util", PathUtil.getJarPathForClass(Iconable.class));
    moduleBuilder.addLibrary("java-analysis-impl", PathUtil.getJarPathForClass(HighlightFixUtil.class));
    moduleBuilder.addLibrary("java-codeserver-highlighter", PathUtil.getJarPathForClass(JavaCompilationError.class));
    moduleBuilder.addLibrary("java-annotations", PathUtil.getJarPathForClass(NotNull.class));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(IntentionDescriptionNotFoundInspection.class);
    myFixture.copyDirectoryToProject("resources", "resources");
  }

  public void testHighlightingForDescription() {
    myFixture.testHighlighting("MyIntentionAction.java");
  }

  public void testHighlightingNotRegisteredInPluginXml() {
    myFixture.testHighlighting("MyIntentionActionNotRegisteredInPluginXml.java");
  }

  public void testHighlightingJavaErrorFixExtension() {
    myFixture.testHighlighting("MyJavaErrorFix.java");
  }

  public void testNoHighlighting() {
    myFixture.copyDirectoryToProject("intentionDescriptions", "intentionDescriptions");
    myFixture.testHighlighting("MyIntentionActionWithDescription.java");
  }

  public void testNoHighlightingModCommand() {
    myFixture.copyDirectoryToProject("intentionDescriptions", "intentionDescriptions");
    myFixture.testHighlighting("MyModCommandIntentionWithDescription.java");
  }

  public void testNoHighlightingDescriptionDirectoryName() {
    myFixture.copyDirectoryToProject("intentionDescriptions", "intentionDescriptions");
    myFixture.testHighlighting("MyIntentionActionWithDescriptionDirectoryName.java");
  }

  public void testHighlightingForBeforeAfter() {
    myFixture.copyDirectoryToProject("intentionDescriptions", "intentionDescriptions");
    myFixture.testHighlighting("MyIntentionActionWithoutBeforeAfter.java");
  }

  public void testHighlightingOptionalBeforeAfter() {
    myFixture.copyDirectoryToProject("intentionDescriptions", "intentionDescriptions");
    myFixture.testHighlighting("MyIntentionActionOptionalBeforeAfter.java");
  }

  public void testQuickFix() {
    myFixture.configureByFile("MyQuickFixIntentionAction.java");
    IntentionAction item = myFixture.findSingleIntention("Create description file description.html");
    myFixture.launchAction(item);

    VirtualFile path = myFixture.findFileInTempDir("intentionDescriptions/MyQuickFixIntentionAction/description.html");
    assertNotNull(path);
    assertTrue(path.exists());
  }

  public void testDescriptionDirectoryNameCompletion() {
    myFixture.copyDirectoryToProject("descriptionDirectoryNameCompletion", "");
    myFixture.testCompletionVariants("plugin.xml",
                                     "IntentionDescriptionDirectory_1", "IntentionDescriptionDirectory_2");
  }
}
