// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the Kotlin/Native wiring of `FaktGenerateTask` (#152, WP4): native leaf mains are consumers,
 * every `KotlinSharedNativeCompilation` (`nativeMain`, `appleMain`, and `commonMain` of an
 * all-native project) is a producer, a lone native target is a single-target task. Real KGP
 * compilations from `ProjectBuilder` with native targets, no mocks. `applyToCompilation` fires
 * under `evaluate()`, so the tasks exist the way a real build registers them.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktNativeTaskWiringTest {

    private fun Project.configureNativeTargets(): Project = also {
        it.getKotlinExtension().apply {
            jvm()
            linuxX64()
            linuxArm64()
            iosArm64()
            iosSimulatorArm64()
        }
        it.evaluate()
    }

    private fun multiTargetProject(): Project = createKmpProject().configureNativeTargets()

    private fun allNativeProject(): Project =
        createKmpProject().also {
            it.getKotlinExtension().apply {
                linuxX64()
                linuxArm64()
            }
            it.evaluate()
        }

    private fun singleTargetProject(): Project =
        createKmpProject().also {
            it.getKotlinExtension().linuxX64()
            it.evaluate()
        }

    private fun Project.generate(name: String): FaktGenerateTask =
        tasks.getByName(name) as FaktGenerateTask

    private fun FaktGenerateTask.context(): SourceSetContext =
        Json.decodeFromString(SourceSetContext.serializer(), sourceSetContextJson.get())

    private fun Project.srcDirsOf(sourceSet: String): Set<File> =
        getKotlinExtension().sourceSets.getByName(sourceSet).kotlin.srcDirs

    private fun Set<File>.holdsPlainDirOf(sourceSet: String): Boolean = any {
        it.path.replace('\\', '/').endsWith("build/generated/fakt/$sourceSet/kotlin")
    }

    private fun Set<File>.holdsOutputOf(task: FaktGenerateTask): Boolean =
        task.generatedKotlinDir.get().asFile in this

    private fun Project.plantMarker(sourceSet: String, name: String): File =
        File(projectDir, "src/$sourceSet/kotlin")
            .apply { mkdirs() }
            .resolve("$name.kt")
            .apply { writeText("// marker\n") }

    private fun Project.metadataCompilation(name: String) =
        getKotlinExtension()
            .targets
            .filterIsInstance<KotlinMetadataTarget>()
            .single()
            .compilations
            .getByName(name)

    // ---- B19: native leaf ---------------------------------------------------------------------

    @Test
    fun `GIVEN a multi-target project WHEN reading the linuxX64 main task THEN it is a leaf native consumer`() {
        val project = multiTargetProject()

        val task = project.generate("faktGenerateLinuxX64Main")

        assertEquals(listOf("linux_x64"), task.konanTargets.get())
        assertFalse(task.sharedNative.getOrElse(false), "a leaf is not a shared native plan")
        assertEquals(project.getKotlinPluginVersion(), task.kotlinNativeVersion.get())
        assertTrue(task.konanHome.isPresent, "konanHome")
        assertTrue(task.refinesKlibs.isEmpty, "a leaf refines nothing")
    }

    @Test
    fun `GIVEN a multi-target project WHEN reading the native leaf mains THEN each names only its own konan target`() {
        val project = multiTargetProject()

        val targets =
            listOf(
                    "faktGenerateLinuxArm64Main",
                    "faktGenerateIosArm64Main",
                    "faktGenerateIosSimulatorArm64Main",
                )
                .associateWith { project.generate(it).konanTargets.get() }

        assertEquals(
            mapOf(
                "faktGenerateLinuxArm64Main" to listOf("linux_arm64"),
                "faktGenerateIosArm64Main" to listOf("ios_arm64"),
                "faktGenerateIosSimulatorArm64Main" to listOf("ios_simulator_arm64"),
            ),
            targets,
        )
    }

    @Test
    fun `GIVEN a multi-target project WHEN reading the linuxX64 main compile THEN nothing orders it after the common producer`() {
        val project = multiTargetProject()

        val explicit =
            project.tasks.getByName("compileKotlinLinuxX64").dependsOn.mapNotNull {
                (it as? Task)?.name ?: it.toString()
            }

        assertTrue(
            explicit.none { "faktGenerateMetadataCommonMain" in it },
            "the hybrid ordering is gone: $explicit",
        )
    }

    @Test
    fun `GIVEN a native leaf WHEN reading its source set roots THEN fragments are available to the worker`() {
        val project = multiTargetProject()

        val roots = project.generate("faktGenerateLinuxX64Main").sourceSetRoots.get()

        assertTrue("linuxX64Main" in roots && "commonMain" in roots, "roots: ${roots.keys}")
    }

    @Test
    fun `GIVEN opt-ins on the member source sets WHEN reading a native task THEN its compiler arguments carry them`() {
        val project = createKmpProject()
        project.getKotlinExtension().apply {
            jvm()
            linuxX64()
            linuxArm64()
            applyDefaultHierarchyTemplate()
            sourceSets.getByName("linuxX64Main").languageSettings.optIn("x.LeafMarker")
            // KGP rejects a dependent set that lacks an opt-in of the set it depends on.
            listOf("nativeMain", "linuxMain", "linuxX64Main", "linuxArm64Main").forEach {
                sourceSets.getByName(it).languageSettings.optIn("x.SharedMarker")
            }
        }
        project.evaluate()

        val leaf = project.generate("faktGenerateLinuxX64Main").compilerArguments.get()
        val shared = project.generate("faktGenerateMetadataNativeMain").compilerArguments.get()

        assertTrue("-opt-in=x.LeafMarker" in leaf, "leaf args: $leaf")
        assertTrue("-opt-in=x.SharedMarker" in leaf, "leaf args: $leaf")
        assertTrue("-opt-in=x.SharedMarker" in shared, "shared args: $shared")
    }

    @Test
    fun `GIVEN a module-wide opt-in WHEN reading the native leaf and shared tasks THEN both carry it`() {
        val project = createKmpProject()
        project.getKotlinExtension().apply {
            jvm()
            linuxX64()
            linuxArm64()
            sourceSets.all { it.languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi") }
        }
        project.evaluate()

        listOf("faktGenerateLinuxX64Main", "faktGenerateMetadataNativeMain").forEach { name ->
            val args = project.generate(name).compilerArguments.get()

            assertTrue("-opt-in=kotlinx.cinterop.ExperimentalForeignApi" in args, "$name: $args")
        }
    }

    // ---- B20 / B21: shared native producers ---------------------------------------------------

    @Test
    fun `GIVEN native targets WHEN reading the shared native producers THEN each is a sorted host-independent shared plan`() {
        val project = multiTargetProject()

        val konanTargets =
            listOf(
                    "faktGenerateMetadataNativeMain",
                    "faktGenerateMetadataLinuxMain",
                    "faktGenerateMetadataAppleMain",
                    "faktGenerateMetadataIosMain",
                )
                .associateWith {
                    val task = project.generate(it)
                    assertTrue(task.sharedNative.get(), "$it sharedNative")
                    task.konanTargets.get()
                }

        assertEquals(
            listOf("linux_arm64", "linux_x64"),
            konanTargets["faktGenerateMetadataLinuxMain"],
        )
        assertEquals(
            listOf("ios_arm64", "ios_simulator_arm64"),
            konanTargets["faktGenerateMetadataIosMain"],
        )
        assertEquals(
            listOf("ios_arm64", "ios_simulator_arm64"),
            konanTargets["faktGenerateMetadataAppleMain"],
        )
        assertEquals(
            listOf("ios_arm64", "ios_simulator_arm64", "linux_arm64", "linux_x64"),
            konanTargets["faktGenerateMetadataNativeMain"],
        )
    }

    @Test
    fun `GIVEN a shared native producer WHEN reading its inputs THEN it sees its own sources and refines the ancestor klibs`() {
        val project = multiTargetProject()
        val native = project.generate("faktGenerateMetadataNativeMain")
        val apple = project.generate("faktGenerateMetadataAppleMain")
        val ownFile = project.plantMarker("appleMain", "AppleMarker")
        project.plantMarker("commonMain", "CommonMarker")
        project.plantMarker("nativeMain", "NativeMarker")
        val commonKlib = project.metadataCompilation("commonMain").output.classesDirs.files
        val nativeKlib = project.metadataCompilation("nativeMain").output.classesDirs.files

        assertEquals(setOf(ownFile), apple.sources.files, "own sources only")
        assertTrue(apple.analysisOnlySources.isEmpty && apple.commonSources.isEmpty)
        assertTrue(commonKlib.all { it in native.refinesKlibs.files }, "nativeMain refines common")
        assertTrue(
            (commonKlib + nativeKlib).all { it in apple.refinesKlibs.files },
            "appleMain refines common and native",
        )
        val deps = apple.commonKlibClasspath.buildDependencies.getDependencies(null).map { it.name }
        assertTrue(
            project.metadataCompilation("commonMain").compileTaskProvider.name in deps,
            "commonKlibClasspath deps: $deps",
        )
    }

    @Test
    fun `GIVEN a shared native producer WHEN decoding its context THEN it routes its own set and holds no host or absolute path`() {
        val project = multiTargetProject()
        val task = project.generate("faktGenerateMetadataNativeMain")

        val json = task.sourceSetContextJson.get()
        val context = task.context()

        assertEquals(mapOf("nativeMain" to "fakt://generated"), context.outputDirectories)
        assertFalse(project.projectDir.path in json, "absolute project path in the context")
        listOf("linux_x64", "macos", "ios_arm64").forEach {
            assertFalse(it in json, "host or konan target '$it' in the context")
        }
    }

    // ---- B22: test ownership ------------------------------------------------------------------

    @Test
    fun `GIVEN native targets WHEN asking who owns the native test sets THEN the native tasks do and no plain dir is added`() {
        val project = multiTargetProject()
        val owners =
            mapOf(
                "linuxX64Test" to "faktGenerateLinuxX64Main",
                "iosArm64Test" to "faktGenerateIosArm64Main",
                "nativeTest" to "faktGenerateMetadataNativeMain",
                "linuxTest" to "faktGenerateMetadataLinuxMain",
                "iosTest" to "faktGenerateMetadataIosMain",
                "appleTest" to "faktGenerateMetadataAppleMain",
            )

        owners.forEach { (testSet, taskName) ->
            val srcDirs = project.srcDirsOf(testSet)

            assertEquals(taskName, testDirOwnerOf(project, testSet), testSet)
            assertTrue(srcDirs.holdsOutputOf(project.generate(taskName)), "$testSet task output")
            assertFalse(srcDirs.holdsPlainDirOf(testSet), "$testSet must not hold the plain dir")
        }
    }

    // ---- B23: all-native commonMain -----------------------------------------------------------

    @Test
    fun `GIVEN an all-native project WHEN reading commonMain THEN it is a shared native producer that owns commonTest`() {
        val project = allNativeProject()

        val task = project.generate("faktGenerateMetadataCommonMain")

        assertTrue(task.sharedNative.get(), "sharedNative")
        assertEquals(listOf("linux_arm64", "linux_x64"), task.konanTargets.get())
        assertEquals("faktGenerateMetadataCommonMain", testDirOwnerOf(project, "commonTest"))
        assertTrue(project.srcDirsOf("commonTest").holdsOutputOf(task), "commonTest task output")
    }

    @Test
    fun `GIVEN an all-native project WHEN reading the leaves THEN they are native consumers owning their tests`() {
        val project = allNativeProject()

        val leaf = project.generate("faktGenerateLinuxX64Main")

        assertEquals(listOf("linux_x64"), leaf.konanTargets.get())
        assertEquals("faktGenerateLinuxX64Main", testDirOwnerOf(project, "linuxX64Test"))
    }

    // ---- B24: single-target -------------------------------------------------------------------

    @Test
    fun `GIVEN a single-target native project WHEN reading its task THEN it owns the common and platform fakes`() {
        val project = singleTargetProject()

        val task = project.generate("faktGenerateLinuxX64Main")

        assertTrue(task.commonGeneratedKotlinDir.isPresent, "commonGeneratedKotlinDir")
        assertEquals(listOf("linux_x64"), task.konanTargets.get())
        assertEquals("faktGenerateLinuxX64Main", testDirOwnerOf(project, "commonTest"))
        assertEquals("faktGenerateLinuxX64Main", testDirOwnerOf(project, "linuxX64Test"))
        assertFalse(project.srcDirsOf("linuxX64Test").holdsPlainDirOf("linuxX64Test"))
        assertTrue(
            project.srcDirsOf("commonTest").contains(task.commonGeneratedKotlinDir.get().asFile),
            "commonTest reads the canonical common dir",
        )
    }

    @Test
    fun `GIVEN a single-target native project WHEN evaluated THEN it is not reported as not cache-correct`() {
        val project = singleTargetProject()

        val extra = project.extensions.extraProperties

        assertFalse(extra.has("fakt.notCacheCorrectWarned"), "the in-process fallback was taken")
        assertNull(project.tasks.findByName("faktGenerateMetadataCommonMain"))
    }

    // ---- B25: leaf test sets of a shared set without a counterpart ----------------------------

    @Test
    fun `GIVEN a shared native set with no test counterpart WHEN wiring its producer THEN every native leaf test set is wired`() {
        val project = createKmpProject()
        project.getKotlinExtension().apply {
            jvm()
            linuxX64()
            linuxArm64()
            val posix = sourceSets.create("posixMain")
            posix.dependsOn(sourceSets.getByName("commonMain"))
            sourceSets.getByName("linuxX64Main").dependsOn(posix)
            sourceSets.getByName("linuxArm64Main").dependsOn(posix)
        }
        project.evaluate()

        val task = project.generate("faktGenerateMetadataPosixMain")

        listOf("linuxX64Test", "linuxArm64Test").forEach { testSet ->
            assertEquals(task.name, testDirOwners(project).bySourceSet[testSet], testSet)
            assertTrue(project.srcDirsOf(testSet).holdsOutputOf(task), "$testSet task output")
        }
    }

    // ---- F4: no silent drop -------------------------------------------------------------------

    @Test
    fun `GIVEN native shared owners in the graph WHEN reading the tasks THEN each maps to a registered task`() {
        listOf(multiTargetProject(), allNativeProject()).forEach { project ->
            val owners = assignSourceSetOwners(readSourceSetGraph(project.getKotlinExtension()))

            val shared = owners.values.filterIsInstance<SourceSetOwner.NativeShared>()

            assertTrue(shared.isNotEmpty(), "the project has native shared sets")
            shared.forEach { owner ->
                val name =
                    "faktGenerateMetadata" + owner.sourceSet.replaceFirstChar { it.uppercase() }
                assertNotNull(project.tasks.findByName(name), "no task '$name' for $owner")
            }
        }
    }

    // ---- audit finding 1: platform-owned native ancestor with an empty leaf --------------------

    @Test
    fun `GIVEN a Fake only in nativeMain of a jvm plus one native target WHEN reading the leaf task THEN nativeMain is an emitted source and commonMain stays analysis only`() {
        val project =
            createKmpProject().also {
                it.getKotlinExtension().apply {
                    jvm()
                    linuxX64()
                }
                it.evaluate()
            }
        val nativeMarker = project.plantMarker("nativeMain", "NativeOnly")
        val commonMarker = project.plantMarker("commonMain", "Common")

        val task = project.generate("faktGenerateLinuxX64Main")

        assertTrue(nativeMarker in task.sources.files, "sources: ${task.sources.files}")
        assertFalse(
            nativeMarker in task.analysisOnlySources.files,
            "analysis only: ${task.analysisOnlySources.files}",
        )
        assertTrue(commonMarker in task.analysisOnlySources.files)
        assertFalse(commonMarker in task.sources.files)
    }

    // ---- B26: laziness ------------------------------------------------------------------------

    @Test
    fun `GIVEN a missing Kotlin Native home WHEN evaluating and realizing the native tasks THEN nothing touches the file system`() {
        val key = "org.gradle.project.kotlin.native.home"
        val previous = System.getProperty(key)
        System.setProperty(key, MISSING_HOME)
        val project =
            try {
                multiTargetProject()
            } finally {
                if (previous == null) System.clearProperty(key)
                else System.setProperty(key, previous)
            }

        val tasks = project.tasks.withType(FaktGenerateTask::class.java).map { it.name }.sorted()
        val home = project.generate("faktGenerateLinuxX64Main").konanHome.get().asFile

        assertTrue("faktGenerateLinuxX64Main" in tasks && "faktGenerateMetadataNativeMain" in tasks)
        assertEquals(File(MISSING_HOME), home)
        assertFalse(home.exists())
    }

    private companion object {
        const val MISSING_HOME = "/does/not/exist"
    }
}
