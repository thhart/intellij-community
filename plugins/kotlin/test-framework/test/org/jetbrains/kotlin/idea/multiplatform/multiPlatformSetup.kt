// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.multiplatform

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.kotlin.checkers.utils.clearFileFromDiagnosticMarkup
import org.jetbrains.kotlin.idea.base.externalSystem.KotlinBuildSystemFacade
import org.jetbrains.kotlin.idea.base.externalSystem.KotlinBuildSystemSourceSet
import org.jetbrains.kotlin.idea.base.platforms.KotlinCommonLibraryKind
import org.jetbrains.kotlin.idea.base.platforms.KotlinJavaScriptLibraryKind
import org.jetbrains.kotlin.idea.base.platforms.KotlinWasmJsLibraryKind
import org.jetbrains.kotlin.idea.base.platforms.KotlinWasmWasiLibraryKind
import org.jetbrains.kotlin.idea.base.plugin.artifacts.TestKotlinArtifacts
import org.jetbrains.kotlin.idea.framework.KotlinSdkType
import org.jetbrains.kotlin.idea.test.*
import org.jetbrains.kotlin.idea.util.application.executeWriteCommand
import org.jetbrains.kotlin.idea.util.sourceRoots
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon
import org.jetbrains.kotlin.platform.isJs
import org.jetbrains.kotlin.platform.js.JsPlatforms
import org.jetbrains.kotlin.platform.jvm.JvmPlatforms
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.platform.konan.NativePlatformWithTarget
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.platform.konan.isNative
import org.jetbrains.kotlin.platform.wasm.WasmPlatforms
import org.jetbrains.kotlin.platform.wasm.isWasmJs
import org.jetbrains.kotlin.platform.wasm.isWasmWasi
import org.jetbrains.kotlin.projectModel.*
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.tooling.core.KotlinToolingVersion
import org.jetbrains.kotlin.utils.closure
import java.io.File

// allows to configure a test mpp project
// testRoot is supposed to contain several directories which contain module sources roots
// configuration is based on those directories names
fun AbstractMultiModuleTest.setupMppProjectFromDirStructure(testRoot: File) {
    assert(testRoot.isDirectory) { testRoot.absolutePath + " must be a directory" }
    val dependencies = dependenciesFile(testRoot)
    if (dependencies.exists()) {
        setupMppProjectFromDependenciesFile(dependencies, testRoot)
        return
    }

    val dirs = testRoot.listFiles().filter { it.isDirectory }
    val rootInfos = dirs.map { parseDirName(it) }
    doSetupProject(rootInfos)
    setupKotlinBuildSystemFacade()
}

fun AbstractMultiModuleTest.setupMppProjectFromTextFile(testRoot: File) {
    assert(testRoot.isDirectory) { testRoot.absolutePath + " must be a directory" }
    val dependencies = dependenciesFile(testRoot)
    setupMppProjectFromDependenciesFile(dependencies, testRoot)
}

private fun dependenciesFile(testRoot: File) = File(testRoot, "dependencies.txt")

fun AbstractMultiModuleTest.setupMppProjectFromDependenciesFile(dependencies: File, testRoot: File) {
    val projectModel = ProjectStructureParser(testRoot).parse(FileUtil.loadFile(dependencies))

    check(projectModel.modules.isNotEmpty()) { "No modules were parsed from dependencies.txt" }

    doSetup(projectModel)
}

fun AbstractMultiModuleTest.doSetup(projectModel: ProjectResolveModel) {
    val resolveModulesToIdeaModules = projectModel.modules.map { resolveModule ->
        val ideaModule = createModule(resolveModule.name)

        addRoot(
            ideaModule,
            resolveModule.root,
            isTestRoot = false,
            transformContainedFiles = { if (it.extension == "kt") clearFileFromDiagnosticMarkup(it) }
        )

        resolveModule.testRoot?.let { testRoot ->
            addRoot(
                ideaModule,
                testRoot,
                isTestRoot = true,
                transformContainedFiles = { if (it.extension == "kt") clearFileFromDiagnosticMarkup(it) }
            )
        }

        resolveModule to ideaModule
    }.toMap()

    for ((resolveModule, ideaModule) in resolveModulesToIdeaModules.entries) {
        val directDependencies: Set<ResolveModule> = resolveModule.dependencies.mapTo(mutableSetOf()) { it.to }

        resolveModule.dependencies.closure(preserveOrder = true) { it.to.dependencies }.forEach {
            when (val dependency = it.to) {
                is ResolveSdk -> {
                    // Only set module SDK if it is specified in module's dependencies explicitly.
                    // Otherwise the last transitive SDK dependency will be written as Module's SDK, which doesn't happen in the real IDE
                    // This check is not lifted to capture an SDK dependency and avoid configuring it as a library or module one
                    if (dependency in directDependencies)
                        setUpSdkForModule(ideaModule, dependency)
                }

                is ResolveLibrary -> ideaModule.addLibrary(
                    jar = dependency.root,
                    name = dependency.name,
                    kind = dependency.kind,
                    sourceJar = dependency.sourceRoot
                )

                else -> ideaModule.addDependency(resolveModulesToIdeaModules[dependency]!!)
            }
        }
    }

    for ((resolveModule, ideaModule) in resolveModulesToIdeaModules.entries) {
        val platform = resolveModule.platform
        val pureKotlinSourceFolders = ideaModule.collectSourceFolders()
        ideaModule.createMultiplatformFacetM3(
            platform,
            dependsOnModuleNames = resolveModule.dependencies.filter { it.kind == ResolveDependency.Kind.DEPENDS_ON }.map { it.to.name },
            pureKotlinSourceFolders = pureKotlinSourceFolders,
            isHmppEnabled = projectModel.mode == ProjectResolveMode.MultiPlatform
        )

        if (projectModel.mode == ProjectResolveMode.MultiPlatform) {
            // New inference is enabled here as these tests are using type refinement feature that is working only along with NI
            ideaModule.enableMultiPlatform(additionalCompilerArguments = "-Xnew-inference " + (resolveModule.additionalCompilerArgs ?: ""))
        }
    }
}

private fun AbstractMultiModuleTest.setUpSdkForModule(ideaModule: Module, sdk: ResolveSdk) {
    when (sdk) {
        FullJdk -> ConfigLibraryUtil.configureSdk(ideaModule, PluginTestCaseBase.addJdk(testRootDisposable) {
            PluginTestCaseBase.jdk(TestJdkKind.FULL_JDK)
        })

        MockJdk -> ConfigLibraryUtil.configureSdk(ideaModule, PluginTestCaseBase.addJdk(testRootDisposable) {
            PluginTestCaseBase.jdk(TestJdkKind.MOCK_JDK)
        })

        KotlinSdk -> {
            KotlinSdkType.setUpIfNeeded(testRootDisposable)
            ConfigLibraryUtil.configureSdk(
                ideaModule,
                runReadAction { ProjectJdkTable.getInstance() }.findMostRecentSdkOfType(KotlinSdkType.INSTANCE)
                    ?: error("Kotlin SDK wasn't created")
            )
        }

        else -> error("Don't know how to set up SDK of type: ${sdk::class}")
    }
}

private fun Module.collectSourceFolders(): List<String> = sourceRoots.map { it.path }

private fun AbstractMultiModuleTest.doSetupProject(rootInfos: List<RootInfo>) {
    val infosByModuleId = rootInfos.groupBy { it.moduleId }
    val modulesById = infosByModuleId.mapValues { (moduleId, infos) ->
        createModuleWithRoots(moduleId, infos)
    }

    infosByModuleId.entries.forEach { (id, rootInfos) ->
        val module = modulesById[id]!!

        if (id.isTest) {
            modulesById[id.copy(isTest = false)]?.let { mainModule ->
                module.addDependency(mainModule)
            }
        }

        rootInfos.flatMap { it.dependencies }.forEach {
            val platform = id.platform
            when (it) {
                is ModuleDependency -> module.addDependency(modulesById[it.moduleId]!!)
                is StdlibDependency -> {
                    when {
                        platform.isCommon() -> module.addLibrary(TestKotlinArtifacts.kotlinStdlibCommon, kind = KotlinCommonLibraryKind)
                        platform.isJvm() -> module.addLibrary(TestKotlinArtifacts.kotlinStdlib)
                        platform.isJs() -> module.addLibrary(TestKotlinArtifacts.kotlinStdlibJs, kind = KotlinJavaScriptLibraryKind)
                        platform.isWasmJs() -> module.addLibrary(TestKotlinArtifacts.kotlinStdlibWasmJs, kind = KotlinWasmJsLibraryKind)
                        platform.isWasmWasi() -> module.addLibrary(
                            TestKotlinArtifacts.kotlinStdlibWasmWasi,
                            kind = KotlinWasmWasiLibraryKind
                        )

                        else -> error("Unknown platform $this")
                    }
                }

                is FullJdkDependency -> {
                    ConfigLibraryUtil.configureSdk(module, PluginTestCaseBase.addJdk(testRootDisposable) {
                        PluginTestCaseBase.jdk(TestJdkKind.FULL_JDK)
                    })
                }

                is CoroutinesDependency -> module.enableCoroutines()
                is KotlinTestDependency -> when {
                    platform.isJvm() -> module.addLibrary(TestKotlinArtifacts.kotlinTestJunit)
                    platform.isJs() -> module.addLibrary(TestKotlinArtifacts.kotlinTestJs, kind = KotlinJavaScriptLibraryKind)
                }
            }
        }
    }

    modulesById.forEach { (nameAndPlatform, module) ->
        val (name, platform, isTest) = nameAndPlatform
        val pureKotlinSourceFolders = module.collectSourceFolders()

        val additionalVisibleModuleNames = buildSet {
            if (isTest) {
                add(nameAndPlatform.copy(isTest = false).ideaModuleName())
            }
        }

        when {
            platform.isCommon() -> {
                module.createMultiplatformFacetM1(
                    platform,
                    useProjectSettings = false,
                    implementedModuleNames = emptyList(),
                    pureKotlinSourceFolders = pureKotlinSourceFolders,
                    additionalVisibleModuleNames = additionalVisibleModuleNames
                )
            }

            else -> {
                val commonModuleId = ModuleId(name, CommonPlatforms.defaultCommonPlatform, isTest)
                val additionalImplementedModules = rootInfos.firstOrNull { it.moduleId == nameAndPlatform }
                    ?.additionalImplementedModules
                    ?.map { it.ideaModuleName() }
                    .orEmpty()

                module.createMultiplatformFacetM1(
                    platform,
                    implementedModuleNames = listOf(commonModuleId.ideaModuleName()) + additionalImplementedModules,
                    pureKotlinSourceFolders = pureKotlinSourceFolders,
                    additionalVisibleModuleNames = additionalVisibleModuleNames
                )

                modulesById[commonModuleId]?.let { commonModule ->
                    module.addDependency(commonModule)
                }
            }
        }
        module.enableMultiPlatform()
    }
}

/**
 * See [KotlinBuildSystemFacade]:
 * - Will use the Module's name as Source Set Name
 * - Will use the module source roots as source directories
 */
private fun AbstractMultiModuleTest.setupKotlinBuildSystemFacade() {
    KotlinBuildSystemFacade.EP_NAME.point.registerExtension(object : KotlinBuildSystemFacade {
        override fun findSourceSet(module: Module) = KotlinBuildSystemSourceSet(
            name = module.name,
            sourceDirectories = module.sourceRoots.map { it.toNioPath() }
        )

        override fun getKotlinToolingVersion(module: Module): KotlinToolingVersion? {
            return null
        }
    }, testRootDisposable)
}

private fun AbstractMultiModuleTest.createModuleWithRoots(
    moduleId: ModuleId,
    infos: List<RootInfo>
): Module {
    val module = createModule(moduleId.ideaModuleName())
    for ((_, isTestRoot, moduleRoot) in infos) {
        addRoot(module, moduleRoot, isTestRoot)
    }

    return module
}

private fun AbstractMultiModuleTest.createModule(name: String): Module {
    val moduleDir = KotlinTestUtils.tmpDirForReusableFolder("kotlinTest")
    val module = createModule("$moduleDir/$name", JavaModuleType.getModuleType())
    val root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(moduleDir)
    checkNotNull(root)
    module.project.executeWriteCommand("refresh") {
        root.refresh(false, true)
    }

    return module
}

private val testSuffixes = setOf("test", "tests")
private val platformNames = mapOf(
    listOf("header", "common", "expect") to CommonPlatforms.defaultCommonPlatform,
    listOf("java", "jvm") to JvmPlatforms.defaultJvmPlatform,
    listOf("java8", "jvm8") to JvmPlatforms.jvm8,
    listOf("java6", "jvm6") to JvmPlatforms.jvm6,
    listOf("nativeCommon") to TargetPlatform(setOf(NativePlatformWithTarget(KonanTarget.LINUX_X64), NativePlatformWithTarget(KonanTarget.MACOS_X64))),
    listOf("js", "javascript") to JsPlatforms.defaultJsPlatform,
    listOf("wasm") to WasmPlatforms.unspecifiedWasmPlatform,
    listOf("native") to NativePlatforms.unspecifiedNativePlatform
)

private fun parseDirName(dir: File): RootInfo {
    val parts = dir.name.split("_")
    return RootInfo(parseModuleId(parts), parseIsTestRoot(parts), dir, parseDependencies(parts), parseImplementations(parts))
}

private fun parseDependencies(parts: List<String>) =
    parts.filter { it.startsWith("dep(") && it.endsWith(")") }.map {
        parseDependency(it)
    }

private fun parseImplementations(parts: List<String>): List<ModuleId> =
    parts.filter { it.startsWith("impl(") && it.endsWith(")") }.map {
        val implString =  it.removePrefix("impl(").removeSuffix(")")
        parseModuleId(implString.split("-"))
    }

private fun parseDependency(it: String): Dependency {
    val dependencyString = it.removePrefix("dep(").removeSuffix(")")

    return when {
        dependencyString.equals("stdlib", ignoreCase = true) -> StdlibDependency
        dependencyString.equals("fulljdk", ignoreCase = true) -> FullJdkDependency
        dependencyString.equals("coroutines", ignoreCase = true) -> CoroutinesDependency
        dependencyString.equals("kotlin-test", ignoreCase = true) -> KotlinTestDependency
        else -> ModuleDependency(parseModuleId(dependencyString.split("-")))
    }
}

private fun parseModuleId(parts: List<String>): ModuleId {
    val platform = parsePlatform(parts)
    val isTest = parseIsTestRoot(parts)
    val name = parseModuleName(parts)
    val id = parseIndex(parts) ?: 0
    assert(id == 0 || !platform.isCommon())
    return ModuleId(name, platform, isTest, id)
}

private fun parsePlatform(parts: List<String>) =
    platformNames.entries.singleOrNull { (names, _) ->
        names.any { name -> parts.any { part -> part.equals(name, ignoreCase = true) } }
    }?.value ?: error("unable to lookup platform among $parts")

private fun parseModuleName(parts: List<String>) = when {
    parts.size > 1 -> parts.first()
    else -> "testModule"
}

private fun parseIsTestRoot(parts: List<String>) =
    testSuffixes.any { suffix -> parts.any { it.equals(suffix, ignoreCase = true) } }

private fun parseIndex(parts: List<String>): Int? {
    return parts.singleOrNull { it.startsWith("id") }?.substringAfter("id")?.toInt()
}

private data class ModuleId(
    val groupName: String,
    val platform: TargetPlatform,
    val isTest: Boolean,
    val index: Int = 0
) {
    fun ideaModuleName(): String = buildString {
        append(groupName)
        append("_${platform.presentableName}")
        if (index != 0) {
            append("_$index")
        }
        if (isTest) {
            append("Test")
        }
    }
}

private val TargetPlatform.presentableName: String
    get() = when {
        isCommon() -> "Common"
        isJvm() -> "JVM"
        isJs() -> "JS"
        isNative() -> "Native"
        else -> error("Unknown platform $this")
    }

private data class RootInfo(
    val moduleId: ModuleId,
    val isTestRoot: Boolean,
    val moduleRoot: File,
    val dependencies: List<Dependency>,
    val additionalImplementedModules: List<ModuleId>,
)

private sealed class Dependency
private class ModuleDependency(val moduleId: ModuleId) : Dependency()
private object StdlibDependency : Dependency()
private object FullJdkDependency : Dependency()
private object CoroutinesDependency : Dependency()
private object KotlinTestDependency : Dependency()
