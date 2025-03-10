// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.k2.refactoring.bindToElement;

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
@TestRoot("refactorings/kotlin.refactorings.tests.k2")
@TestDataPath("$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
@TestMetadata("../../idea/tests/testData/refactoring/bindToFqn")
public abstract class K2BindToFqnTestGenerated extends AbstractK2BindToFqnTest {
    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/annotationCall")
    public static class AnnotationCall extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/annotationCall/FullyQualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/annotationCall/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/annotationCall/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/annotationReference")
    public static class AnnotationReference extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/annotationReference/FullyQualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/annotationReference/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/annotationReference/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/callOnCompanionObject")
    public static class CallOnCompanionObject extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/callOnCompanionObject/FullyQualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/callOnCompanionObject/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/callOnCompanionObject/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/callOnObject")
    public static class CallOnObject extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/callOnObject/FullyQualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/callOnObject/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/callOnObject/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/constructorCall")
    public static class ConstructorCall extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("ChangeImport.kt")
        public void testChangeImport() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/constructorCall/ChangeImport.kt");
        }

        @TestMetadata("ChangeImportToRootPkg.kt")
        public void testChangeImportToRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/constructorCall/ChangeImportToRootPkg.kt");
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/constructorCall/FullyQualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/constructorCall/RootPkg.kt");
        }

        @TestMetadata("TypeArgument.kt")
        public void testTypeArgument() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/constructorCall/TypeArgument.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/constructorCall/UnQualified.kt");
        }

        @TestMetadata("UnQualifiedInCallChain.kt")
        public void testUnQualifiedInCallChain() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/constructorCall/UnQualifiedInCallChain.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/delegatedSuperTypeReference")
    public static class DelegatedSuperTypeReference extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/delegatedSuperTypeReference/FullyQualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/delegatedSuperTypeReference/RootPkg.kt");
        }

        @TestMetadata("Unqualified.kt")
        public void testUnqualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/delegatedSuperTypeReference/Unqualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/importReference")
    public static class ImportReference extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("ImportReference.kt")
        public void testImportReference() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/importReference/ImportReference.kt");
        }

        @TestMetadata("ImportReferenceWithAlias.kt")
        public void testImportReferenceWithAlias() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/importReference/ImportReferenceWithAlias.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/objectProperty")
    public static class ObjectProperty extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/objectProperty/FullyQualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/objectProperty/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/objectProperty/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/packageVsDeclarationCollision")
    public static class PackageVsDeclarationCollision extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("PackageVsPropertyOnCallableReference.kt")
        public void testPackageVsPropertyOnCallableReference() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/packageVsDeclarationCollision/PackageVsPropertyOnCallableReference.kt");
        }

        @TestMetadata("PackageVsPropertyOnFunctionCall.kt")
        public void testPackageVsPropertyOnFunctionCall() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/packageVsDeclarationCollision/PackageVsPropertyOnFunctionCall.kt");
        }

        @TestMetadata("PackageVsPropertyOnFunctionCallFailedImport.kt")
        public void testPackageVsPropertyOnFunctionCallFailedImport() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/packageVsDeclarationCollision/PackageVsPropertyOnFunctionCallFailedImport.kt");
        }

        @TestMetadata("PackageVsPropertyOnNameReference.kt")
        public void testPackageVsPropertyOnNameReference() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/packageVsDeclarationCollision/PackageVsPropertyOnNameReference.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/propertyTypeReference")
    public static class PropertyTypeReference extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/propertyTypeReference/FullyQualified.kt");
        }

        @TestMetadata("FullyQualifiedWithLongerFqn.kt")
        public void testFullyQualifiedWithLongerFqn() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/propertyTypeReference/FullyQualifiedWithLongerFqn.kt");
        }

        @TestMetadata("FullyQualifiedWithShorterFqn.kt")
        public void testFullyQualifiedWithShorterFqn() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/propertyTypeReference/FullyQualifiedWithShorterFqn.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/propertyTypeReference/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/propertyTypeReference/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/returnTypeReference")
    public static class ReturnTypeReference extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/returnTypeReference/FullyQualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/returnTypeReference/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/returnTypeReference/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/superTypeCall")
    public static class SuperTypeCall extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/superTypeCall/FullyQualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/superTypeCall/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/superTypeCall/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/superTypeReference")
    public static class SuperTypeReference extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("FullyQualified.kt")
        public void testFullyQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/superTypeReference/FullyQualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/superTypeReference/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/superTypeReference/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/topLevelFunctionCall")
    public static class TopLevelFunctionCall extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("Qualified.kt")
        public void testQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/topLevelFunctionCall/Qualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/topLevelFunctionCall/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/topLevelFunctionCall/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/topLevelProperty")
    public static class TopLevelProperty extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("ChangeImportToRootPkg.kt")
        public void testChangeImportToRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/topLevelProperty/ChangeImportToRootPkg.kt");
        }

        @TestMetadata("Qualified.kt")
        public void testQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/topLevelProperty/Qualified.kt");
        }

        @TestMetadata("RootPkg.kt")
        public void testRootPkg() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/topLevelProperty/RootPkg.kt");
        }

        @TestMetadata("UnQualified.kt")
        public void testUnQualified() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/topLevelProperty/UnQualified.kt");
        }
    }

    @RunWith(JUnit3RunnerWithInners.class)
    @TestMetadata("../../idea/tests/testData/refactoring/bindToFqn/typeArgs")
    public static class TypeArgs extends AbstractK2BindToFqnTest {
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public final KotlinPluginMode getPluginMode() {
            return KotlinPluginMode.K2;
        }

        private void runTest(String testDataFilePath) throws Exception {
            KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
        }

        @TestMetadata("AllTypeArguments.kt")
        public void testAllTypeArguments() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/typeArgs/AllTypeArguments.kt");
        }

        @TestMetadata("FirstLastTypeArguments.kt")
        public void testFirstLastTypeArguments() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/typeArgs/FirstLastTypeArguments.kt");
        }

        @TestMetadata("SingleRef.kt")
        public void testSingleRef() throws Exception {
            runTest("../../idea/tests/testData/refactoring/bindToFqn/typeArgs/SingleRef.kt");
        }
    }
}
