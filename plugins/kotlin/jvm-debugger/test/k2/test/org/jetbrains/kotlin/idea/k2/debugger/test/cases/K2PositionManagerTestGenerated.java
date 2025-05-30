// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.k2.debugger.test.cases;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.idea.base.plugin.KotlinPluginMode;
import org.jetbrains.kotlin.idea.base.test.TestRoot;
import org.jetbrains.kotlin.idea.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.idea.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

/**
 * This class is generated by {@link org.jetbrains.kotlin.testGenerator.generator.TestGenerator}.
 * DO NOT MODIFY MANUALLY.
 */
@SuppressWarnings("all")
@TestRoot("jvm-debugger/test/k2")
@TestDataPath("$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public abstract class K2PositionManagerTestGenerated extends AbstractK2PositionManagerTest {
    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../testData/positionManager")
    public static class SingleFile extends AbstractK2PositionManagerTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("anonymousFunction.kt")
        public void testAnonymousFunction() throws Exception {
            runTest("../testData/positionManager/anonymousFunction.kt");
        }

        @TestMetadata("anonymousNamedFunction.kt")
        public void testAnonymousNamedFunction() throws Exception {
            runTest("../testData/positionManager/anonymousNamedFunction.kt");
        }

        @TestMetadata("class.kt")
        public void testClass() throws Exception {
            runTest("../testData/positionManager/class.kt");
        }

        @TestMetadata("classObject.kt")
        public void testClassObject() throws Exception {
            runTest("../testData/positionManager/classObject.kt");
        }

        @TestMetadata("enum.kt")
        public void testEnum() throws Exception {
            runTest("../testData/positionManager/enum.kt");
        }

        @TestMetadata("extensionFunction.kt")
        public void testExtensionFunction() throws Exception {
            runTest("../testData/positionManager/extensionFunction.kt");
        }

        @TestMetadata("functionLiteral.kt")
        public void testFunctionLiteral() throws Exception {
            runTest("../testData/positionManager/functionLiteral.kt");
        }

        @TestMetadata("functionLiteralInVal.kt")
        public void testFunctionLiteralInVal() throws Exception {
            runTest("../testData/positionManager/functionLiteralInVal.kt");
        }

        @TestMetadata("innerClass.kt")
        public void testInnerClass() throws Exception {
            runTest("../testData/positionManager/innerClass.kt");
        }

        @TestMetadata("interface.kt")
        public void testInterface() throws Exception {
            runTest("../testData/positionManager/interface.kt");
        }

        @TestMetadata("JvmNameAnnotation.kt")
        public void testJvmNameAnnotation() throws Exception {
            runTest("../testData/positionManager/JvmNameAnnotation.kt");
        }

        @TestMetadata("localFunction.kt")
        public void testLocalFunction() throws Exception {
            runTest("../testData/positionManager/localFunction.kt");
        }

        @TestMetadata("objectDeclaration.kt")
        public void testObjectDeclaration() throws Exception {
            runTest("../testData/positionManager/objectDeclaration.kt");
        }

        @TestMetadata("objectExpression.kt")
        public void testObjectExpression() throws Exception {
            runTest("../testData/positionManager/objectExpression.kt");
        }

        @TestMetadata("package.kt")
        public void testPackage() throws Exception {
            runTest("../testData/positionManager/package.kt");
        }

        @TestMetadata("propertyAccessor.kt")
        public void testPropertyAccessor() throws Exception {
            runTest("../testData/positionManager/propertyAccessor.kt");
        }

        @TestMetadata("propertyInitializer.kt")
        public void testPropertyInitializer() throws Exception {
            runTest("../testData/positionManager/propertyInitializer.kt");
        }

        @TestMetadata("topLevelPropertyInitializer.kt")
        public void testTopLevelPropertyInitializer() throws Exception {
            runTest("../testData/positionManager/topLevelPropertyInitializer.kt");
        }

        @TestMetadata("twoClasses.kt")
        public void testTwoClasses() throws Exception {
            runTest("../testData/positionManager/twoClasses.kt");
        }

        @TestMetadata("_DefaultPackage.kt")
        public void test_DefaultPackage() throws Exception {
            runTest("../testData/positionManager/_DefaultPackage.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../testData/positionManager")
    public static class MultiFile extends AbstractK2PositionManagerTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("multiFilePackage")
        public void testMultiFilePackage() throws Exception {
            runTest("../testData/positionManager/multiFilePackage/");
        }

        @TestMetadata("multiFileSameName")
        public void testMultiFileSameName() throws Exception {
            runTest("../testData/positionManager/multiFileSameName/");
        }
    }
}
