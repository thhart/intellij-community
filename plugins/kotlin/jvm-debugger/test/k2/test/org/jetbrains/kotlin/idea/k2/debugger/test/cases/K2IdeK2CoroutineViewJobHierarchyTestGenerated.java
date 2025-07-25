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
@TestMetadata("../testData/coroutinesView")
public class K2IdeK2CoroutineViewJobHierarchyTestGenerated extends AbstractK2IdeK2CoroutineViewJobHierarchyTest {
    @java.lang.Override
    @org.jetbrains.annotations.NotNull
    public final KotlinPluginMode getPluginMode() {
        return KotlinPluginMode.K2;
    }

    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    @TestMetadata("coroutinesHierarchy2.kt")
    public void testCoroutinesHierarchy2() throws Exception {
        runTest("../testData/coroutinesView/coroutinesHierarchy2.kt");
    }

    @TestMetadata("coroutinesHierarchy3.kt")
    public void testCoroutinesHierarchy3() throws Exception {
        runTest("../testData/coroutinesView/coroutinesHierarchy3.kt");
    }

    @TestMetadata("oneCoroutine.kt")
    public void testOneCoroutine() throws Exception {
        runTest("../testData/coroutinesView/oneCoroutine.kt");
    }
}
