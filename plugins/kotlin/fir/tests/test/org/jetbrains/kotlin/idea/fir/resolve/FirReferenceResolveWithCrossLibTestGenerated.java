// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.fir.resolve;

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
@TestRoot("fir/tests")
@TestDataPath("$CONTENT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
@TestMetadata("../../idea/tests/testData/resolve/referenceWithLib")
public class FirReferenceResolveWithCrossLibTestGenerated extends AbstractFirReferenceResolveWithCrossLibTest {
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public final KotlinPluginMode getPluginMode() {
        return KotlinPluginMode.K2;
    }

    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    @TestMetadata("dataClassSyntheticMethods")
    public void testDataClassSyntheticMethods() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/dataClassSyntheticMethods/");
    }

    @TestMetadata("delegatedPropertyWithTypeParameters")
    public void testDelegatedPropertyWithTypeParameters() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/delegatedPropertyWithTypeParameters/");
    }

    @TestMetadata("enumEntryMethods")
    public void testEnumEntryMethods() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/enumEntryMethods/");
    }

    @TestMetadata("enumSyntheticMethods")
    public void testEnumSyntheticMethods() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/enumSyntheticMethods/");
    }

    @TestMetadata("fakeOverride")
    public void testFakeOverride() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/fakeOverride/");
    }

    @TestMetadata("fakeOverride2")
    public void testFakeOverride2() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/fakeOverride2/");
    }

    @TestMetadata("infinityAndNanInJavaAnnotation")
    public void testInfinityAndNanInJavaAnnotation() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/infinityAndNanInJavaAnnotation/");
    }

    @TestMetadata("innerClassFromLib")
    public void testInnerClassFromLib() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/innerClassFromLib/");
    }

    @TestMetadata("iteratorWithTypeParameter")
    public void testIteratorWithTypeParameter() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/iteratorWithTypeParameter/");
    }

    @TestMetadata("multiDeclarationWithTypeParameters")
    public void testMultiDeclarationWithTypeParameters() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/multiDeclarationWithTypeParameters/");
    }

    @TestMetadata("namedArguments")
    public void testNamedArguments() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/namedArguments/");
    }

    @TestMetadata("nestedClassFromLib")
    public void testNestedClassFromLib() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/nestedClassFromLib/");
    }

    @TestMetadata("overloadFun")
    public void testOverloadFun() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/overloadFun/");
    }

    @TestMetadata("overridingFunctionWithSamAdapter")
    public void testOverridingFunctionWithSamAdapter() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/overridingFunctionWithSamAdapter/");
    }

    @TestMetadata("packageOfLibDeclaration")
    public void testPackageOfLibDeclaration() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/packageOfLibDeclaration/");
    }

    @TestMetadata("referenceToRootJavaClassFromLib")
    public void testReferenceToRootJavaClassFromLib() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/referenceToRootJavaClassFromLib/");
    }

    @TestMetadata("sameNameInLib")
    public void testSameNameInLib() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/sameNameInLib/");
    }

    @TestMetadata("setWithTypeParameters")
    public void testSetWithTypeParameters() throws Exception {
        runTest("../../idea/tests/testData/resolve/referenceWithLib/setWithTypeParameters/");
    }
}
