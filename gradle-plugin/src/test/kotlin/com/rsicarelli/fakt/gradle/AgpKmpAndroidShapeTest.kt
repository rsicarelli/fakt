// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.android.build.api.dsl.LibraryExtension
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.gradle.helpers.evaluate
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Contract suite (#163) for `androidTarget()` driven by the REAL Android Gradle Plugin and the real
 * Kotlin Gradle Plugin inside `ProjectBuilder` (AGP evaluates with an empty `sdk.dir`). Everything
 * is asserted after evaluation, because KGP makes the associations and `dependsOn` edges late.
 *
 * The first block pins the KGP/AGP facts Fakt's routing relies on (B12); the second pins how Fakt
 * wires them (B13-B16).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgpKmpAndroidShapeTest {

    @TempDir lateinit var tempDir: File

    private fun androidProject(
        flavors: Boolean = false,
        flavorNames: List<String> = listOf("free", "paid"),
        withJvm: Boolean = true,
        configure: (Project) -> Unit = {},
    ): Project {
        val dir = File(tempDir, "p${counter++}").also { it.mkdirs() }
        val sdk = File(dir, "emptysdk").also { it.mkdirs() }
        File(dir, "local.properties").writeText("sdk.dir=${sdk.absolutePath}\n")
        val project = ProjectBuilder.builder().withProjectDir(dir).build()
        project.group = "com.example"
        project.version = "1.0.0"
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply("com.android.library")
        project.pluginManager.apply("com.rsicarelli.fakt")
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        if (withJvm) kotlin.jvm()
        kotlin.androidTarget()
        val android = project.extensions.getByType(LibraryExtension::class.java)
        android.namespace = "demo.shape"
        android.compileSdk = 34
        if (flavors) {
            android.flavorDimensions += "tier"
            flavorNames.forEach { name ->
                android.productFlavors.create(name) { it.dimension = "tier" }
            }
        }
        configure(project)
        listOf("androidDebug", "androidMain", "commonMain", "androidRelease").forEach {
            project.plantMarker(it)
        }
        project.evaluate()
        return project
    }

    private fun Project.plantMarker(sourceSet: String) {
        File(projectDir, "src/$sourceSet/kotlin")
            .apply { mkdirs() }
            .resolve("Marker.kt")
            .writeText("// marker\n")
    }

    private fun Project.kmp(): KotlinMultiplatformExtension =
        extensions.getByType(KotlinMultiplatformExtension::class.java)

    private fun Project.android(name: String): KotlinCompilation<*> =
        kmp().targets.getByName("android").compilations.getByName(name)

    private fun Project.fakt(name: String): FaktGenerateTask =
        tasks.getByName(name) as FaktGenerateTask

    private fun Project.context(name: String): SourceSetContext =
        Json.decodeFromString(SourceSetContext.serializer(), fakt(name).sourceSetContextJson.get())

    private fun Project.relative(files: Set<File>): Set<String> =
        files.map { it.relativeTo(projectDir).path }.toSet()

    private fun Project.srcDirs(sourceSet: String): Set<String> =
        kmp()
            .sourceSets
            .getByName(sourceSet)
            .kotlin
            .srcDirs
            .map { it.relativeTo(projectDir).path }
            .toSet()

    /**
     * The tasks building the Kotlin srcDirs of [sourceSet], i.e. what every compilation of that set
     * waits for. The compile tasks themselves cannot be resolved here: AGP wants an installed SDK.
     */
    private fun Project.dependencyNames(sourceSet: String): Set<String> =
        kmp()
            .sourceSets
            .getByName(sourceSet)
            .kotlin
            .buildDependencies
            .getDependencies(null)
            .map { it.name }
            .toSet()

    // B12: the KGP/AGP contract

    @Test
    fun `GIVEN jvm and androidTarget WHEN evaluating THEN the android target has one compilation per variant and test kind`() {
        val project = androidProject()

        val names = project.kmp().targets.getByName("android").compilations.map { it.name }.toSet()

        assertEquals(
            setOf("debug", "release", "debugUnitTest", "releaseUnitTest", "debugAndroidTest"),
            names,
        )
    }

    @Test
    fun `GIVEN a debug variant WHEN reading its source sets THEN androidDebug is the default and androidMain a sibling member`() {
        val project = androidProject()
        val debug = project.android("debug")

        assertEquals("androidDebug", debug.defaultSourceSet.name)
        assertTrue(
            debug.kotlinSourceSets
                .map { it.name }
                .containsAll(listOf("androidDebug", "androidMain"))
        )
        val dependsOn = project.kmp().sourceSets.getByName("androidDebug").dependsOn.map { it.name }
        assertFalse(
            "androidMain" in dependsOn,
            "androidDebug must not depend on androidMain: $dependsOn",
        )
    }

    @Test
    fun `GIVEN test compilations WHEN reading their associations THEN the unit tests and android tests are associated with the debug variant`() {
        val project = androidProject()

        assertEquals(
            setOf("debug"),
            project.android("debugUnitTest").associatedCompilations.map { it.name }.toSet(),
        )
        assertEquals(
            setOf("debug"),
            project.android("debugAndroidTest").associatedCompilations.map { it.name }.toSet(),
        )
        assertEquals(
            setOf("release"),
            project.android("releaseUnitTest").associatedCompilations.map { it.name }.toSet(),
        )
    }

    @Test
    fun `GIVEN jvm and androidTarget WHEN reading the metadata target THEN it has a commonMain compilation`() {
        val project = androidProject()

        val metadata = project.kmp().targets.getByName("metadata").compilations.map { it.name }

        assertTrue("commonMain" in metadata, "$metadata")
    }

    @Test
    fun `GIVEN only androidTarget WHEN reading the metadata target THEN it has no commonMain compilation`() {
        val project = androidProject(withJvm = false)

        val metadata = project.kmp().targets.getByName("metadata").compilations.map { it.name }

        assertFalse("commonMain" in metadata, "$metadata")
    }

    // B13-B16: Fakt wiring

    @Test
    fun `GIVEN jvm and androidTarget WHEN evaluating THEN a consumer task exists per main variant and none for test variants`() {
        val project = androidProject()

        val names = project.tasks.names.filter { it.startsWith("faktGenerate") }.toSet()

        assertTrue(
            names.containsAll(setOf("faktGenerateAndroidDebug", "faktGenerateAndroidRelease")),
            "$names",
        )
        assertFalse(names.any { it.contains("UnitTest") || it.contains("AndroidTest") }, "$names")
    }

    @Test
    fun `GIVEN the debug consumer WHEN reading its sources THEN androidDebug and androidMain are own sources`() {
        val project = androidProject()

        val own = project.relative(project.fakt("faktGenerateAndroidDebug").sources.files)

        assertEquals(
            setOf("src/androidDebug/kotlin/Marker.kt", "src/androidMain/kotlin/Marker.kt"),
            own,
        )
    }

    @Test
    fun `GIVEN the debug consumer WHEN reading its analysis-only sources THEN commonMain is the common fragment`() {
        val project = androidProject()

        val analysis =
            project.relative(project.fakt("faktGenerateAndroidDebug").analysisOnlySources.files)

        assertEquals(setOf("src/commonMain/kotlin/Marker.kt"), analysis)
    }

    @Test
    fun `GIVEN the debug consumer WHEN reading its route THEN it emits androidDebug and androidMain into the variant directory`() {
        val project = androidProject()

        val context = project.context("faktGenerateAndroidDebug")

        assertEquals(setOf("androidDebug", "androidMain"), context.outputDirectories.keys)
        assertEquals(
            "build/generated/fakt/android/debug/kotlin",
            project
                .relative(
                    setOf(project.fakt("faktGenerateAndroidDebug").generatedKotlinDir.get().asFile)
                )
                .single(),
        )
    }

    @Test
    fun `GIVEN per-variant consumers WHEN reading the android test source sets THEN each gets only its variant output`() {
        val project = androidProject()
        val debug = "build/generated/fakt/android/debug/kotlin"
        val release = "build/generated/fakt/android/release/kotlin"

        assertTrue(
            debug in project.srcDirs("androidUnitTestDebug"),
            "${project.srcDirs("androidUnitTestDebug")}",
        )
        assertTrue(debug in project.srcDirs("androidInstrumentedTestDebug"))
        assertTrue(release in project.srcDirs("androidUnitTestRelease"))
        assertFalse(debug in project.srcDirs("androidUnitTestRelease"))
        assertFalse(release in project.srcDirs("androidUnitTestDebug"))
    }

    @Test
    fun `GIVEN per-variant consumers WHEN reading the shared test source sets THEN none holds a variant or plain generated directory`() {
        val project = androidProject()

        listOf("androidUnitTest", "androidInstrumentedTest", "commonTest").forEach { set ->
            val dirs = project.srcDirs(set).filter { it.startsWith("build/generated/fakt") }
            val allowed =
                if (set == "commonTest") setOf("build/generated/fakt/commonTest/kotlin")
                else emptySet()
            assertTrue(dirs.none { "/android/" in it }, "$set holds a variant directory: $dirs")
            assertTrue(allowed.containsAll(dirs), "$set holds an unexpected directory: $dirs")
        }
    }

    @Test
    fun `GIVEN per-variant consumers WHEN reading task dependencies THEN a unit test compile waits for its own variant only`() {
        val project = androidProject()

        val debugTest = project.dependencyNames("androidUnitTestDebug")
        val debugMain =
            project.dependencyNames("androidDebug") + project.dependencyNames("androidMain")

        assertTrue("faktGenerateAndroidDebug" in debugTest, "$debugTest")
        assertFalse("faktGenerateAndroidRelease" in debugTest, "$debugTest")
        assertTrue(debugMain.none { it.startsWith("faktGenerate") }, "$debugMain")
    }

    @Test
    fun `GIVEN a routed androidTarget WHEN evaluating THEN the in-process fallback warning is never raised`() {
        val project = androidProject()

        assertNull(
            project.extensions.extraProperties.let {
                if (it.has("fakt.notCacheCorrectWarned")) it.get("fakt.notCacheCorrectWarned")
                else null
            }
        )
    }

    @Test
    fun `GIVEN product flavors WHEN evaluating THEN a flavored variant routes the default flavor and build type sets`() {
        val project = androidProject(flavors = true)

        val context = project.context("faktGenerateAndroidFreeDebug")

        assertEquals(
            setOf("androidFreeDebug", "androidMain", "androidFree", "androidDebug"),
            context.outputDirectories.keys,
        )
    }

    @Test
    fun `GIVEN flavors named latest and contest WHEN evaluating THEN their variants stay main and route their sets`() {
        val project = androidProject(flavors = true, flavorNames = listOf("latest", "contest"))

        val latest = project.context("faktGenerateAndroidLatestDebug").outputDirectories.keys
        val contest = project.context("faktGenerateAndroidContestRelease").outputDirectories.keys

        assertEquals(
            setOf("androidLatestDebug", "androidMain", "androidLatest", "androidDebug"),
            latest,
        )
        assertEquals(
            setOf("androidContestRelease", "androidMain", "androidContest", "androidRelease"),
            contest,
        )
    }

    @Test
    fun `GIVEN opt-in on androidMain WHEN reading a variant task THEN the worker arguments carry it`() {
        val project = androidProject {
            val main = it.kmp().sourceSets.getByName("androidMain")
            main.languageSettings.optIn("y.Marker")
        }

        val args = project.fakt("faktGenerateAndroidDebug").compilerArguments.get()

        assertTrue("-opt-in=y.Marker" in args, "args: $args")
    }

    @Test
    fun `GIVEN androidMain under a custom intermediate WHEN reading the variant roots THEN none are given so the worker stays flat`() {
        val project = androidProject {
            val sets = it.kmp().sourceSets
            val shared = sets.create("jvmAndAndroidMain")
            shared.dependsOn(sets.getByName("commonMain"))
            sets.getByName("androidMain").dependsOn(shared)
            sets.getByName("jvmMain").dependsOn(shared)
            it.plantMarker("jvmAndAndroidMain")
        }

        val android = project.fakt("faktGenerateAndroidDebug").sourceSetRoots.get()
        val jvm = project.fakt("faktGenerateJvmMain").sourceSetRoots.get()

        assertEquals(emptyMap(), android)
        assertTrue("jvmMain" in jvm.keys, "$jvm")
    }

    @Test
    fun `GIVEN only androidTarget WHEN evaluating THEN the synthetic commonMain producer writes the canonical commonTest directory`() {
        val project = androidProject(withJvm = false)

        val producer = project.fakt("faktGenerateCommonMain")

        assertEquals(
            "build/generated/fakt/commonTest/kotlin",
            project.relative(setOf(producer.generatedKotlinDir.get().asFile)).single(),
        )
        assertEquals(
            setOf("src/commonMain/kotlin/Marker.kt"),
            project.relative(producer.commonSources.files),
        )
        assertTrue("faktGenerateCommonMain" in project.dependencyNames("commonTest"))
    }

    @Test
    fun `GIVEN only androidTarget WHEN reading the consumer route THEN commonMain is left to the synthetic producer`() {
        val project = androidProject(withJvm = false)

        val keys = project.context("faktGenerateAndroidDebug").outputDirectories.keys

        assertFalse("commonMain" in keys, "$keys")
        assertNotNull(project.tasks.findByName("faktGenerateAndroidRelease"))
    }

    @Test
    fun `GIVEN fakt disabled WHEN reading the consumer inputs THEN no source is fed to the task`() {
        val project = androidProject {
            it.extensions.getByType(FaktPluginExtension::class.java).enabled.set(false)
        }

        val task = project.fakt("faktGenerateAndroidDebug")

        assertTrue(task.sources.files.isEmpty())
        assertTrue(task.analysisOnlySources.files.isEmpty())
    }

    // Resolving the Android classes-jar view needs an installed SDK, which ProjectBuilder does not
    // have: the sample (samples/kmp-android-target, AarTypeSource) proves the AAR resolution, and
    // these two rows pin the wiring decision.
    @Test
    fun `GIVEN an android compilation WHEN reading the worker dependencies THEN they are not the raw variant files`() {
        val project = androidProject()
        val debug = project.android("debug")

        val files = debug.workerDependencyFiles()

        assertTrue(files !== debug.compileDependencyFiles)
    }

    @Test
    fun `GIVEN a jvm compilation WHEN reading the worker dependencies THEN they are the compile dependency files`() {
        val project = androidProject()
        val main = project.kmp().targets.getByName("jvm").compilations.getByName("main")

        val files = main.workerDependencyFiles()

        assertTrue(files === main.compileDependencyFiles)
    }

    private companion object {
        var counter = 0
    }
}
