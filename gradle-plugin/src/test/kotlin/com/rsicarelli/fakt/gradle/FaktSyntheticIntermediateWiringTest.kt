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
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val SHARED_TASK = "faktGenerateDesktopAndServerMain"
private const val SHARED_OUTPUT = "generated/fakt/synthetic/desktopAndServerMain/kotlin"

/**
 * Pins the synthetic producer of a JVM-only intermediate source set (#162): `desktop` + `server`
 * share `desktopAndServerMain`, KGP builds no usable metadata compilation for it, so one
 * `faktGenerateDesktopAndServerMain` task on the representative (`desktop`) owns its fakes.
 *
 * KGP calls `applyToCompilation` under `ProjectBuilder`, so `evaluate()` registers the tasks the
 * way a real build does.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktSyntheticIntermediateWiringTest {

    private fun Project.faktExtension(): FaktPluginExtension =
        extensions.getByType(FaktPluginExtension::class.java)

    /** `commonMain` <- `desktopAndServerMain` <- `desktopMain` and `serverMain`. */
    private fun org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension.shareDesktopAndServer(
        withTest: Boolean
    ) {
        val shared = sourceSets.create("desktopAndServerMain")
        shared.dependsOn(sourceSets.getByName("commonMain"))
        sourceSets.getByName("desktopMain").dependsOn(shared)
        sourceSets.getByName("serverMain").dependsOn(shared)
        if (withTest) {
            val sharedTest = sourceSets.create("desktopAndServerTest")
            sharedTest.dependsOn(sourceSets.getByName("commonTest"))
            sourceSets.getByName("desktopTest").dependsOn(sharedTest)
            sourceSets.getByName("serverTest").dependsOn(sharedTest)
        }
    }

    private fun sharedProject(withTest: Boolean = false, withJs: Boolean = false): Project =
        createKmpProject().also {
            it.getKotlinExtension().apply {
                jvm("desktop")
                jvm("server")
                if (withJs) js { nodejs() }
                shareDesktopAndServer(withTest)
            }
            it.evaluate()
        }

    private fun Project.plantMarker(sourceSet: String, name: String): File =
        File(projectDir, "src/$sourceSet/kotlin")
            .apply { mkdirs() }
            .resolve("$name.kt")
            .apply { writeText("// marker\n") }

    private fun Project.sharedTask(): FaktGenerateTask =
        tasks.getByName(SHARED_TASK) as FaktGenerateTask

    private fun FaktGenerateTask.context(): SourceSetContext =
        Json.decodeFromString(SourceSetContext.serializer(), sourceSetContextJson.get())

    private fun Project.srcDirsOf(sourceSet: String): Set<File> =
        getKotlinExtension().sourceSets.getByName(sourceSet).kotlin.srcDirs

    private fun Project.depsOf(sourceSet: String): List<String> =
        getKotlinExtension()
            .sourceSets
            .getByName(sourceSet)
            .kotlin
            .buildDependencies
            .getDependencies(null)
            .map { it.name }

    private fun Set<File>.hasOutput(path: String): Boolean = any { it.path.endsWith(path) }

    // ---- B13 ----------------------------------------------------------------------------------

    @Test
    fun `GIVEN desktop and server sharing an intermediate WHEN evaluated THEN exactly one task for it exists on the representative`() {
        val project = sharedProject()

        assertEquals(1, project.tasks.names.count { it == SHARED_TASK })
        val context = project.sharedTask().context()
        assertEquals("desktop", context.targetName)
        assertEquals("main", context.compilationName)
        assertEquals("desktopMain", context.defaultSourceSet.name)
    }

    @Test
    fun `GIVEN the intermediate task WHEN reading its sources THEN the owned set is emitted and the rest is analysis only`() {
        val project = sharedProject()
        val common = project.plantMarker("commonMain", "CommonMarker")
        val shared = project.plantMarker("desktopAndServerMain", "SharedMarker")
        val desktop = project.plantMarker("desktopMain", "DesktopMarker")
        val server = project.plantMarker("serverMain", "ServerMarker")

        val task = project.sharedTask()

        assertEquals(setOf(shared), task.sources.files, "sources")
        assertTrue(task.commonSources.files.isEmpty(), "commonSources: ${task.commonSources.files}")
        assertEquals(setOf(common), task.analysisOnlySources.files, "analysisOnlySources")
        assertEquals(setOf(desktop), task.platformAnalysisOnlySources.files, "platformAnalysis")
        assertFalse(server in task.platformAnalysisOnlySources.files, "server is not analysed")
    }

    @Test
    fun `GIVEN the intermediate task WHEN decoding its context THEN it routes the owned set to the generated token`() {
        val project = sharedProject()

        val context = project.sharedTask().context()

        assertEquals(mapOf("desktopAndServerMain" to "fakt://generated"), context.outputDirectories)
        assertEquals(
            setOf("desktopMain", "desktopAndServerMain", "commonMain"),
            context.allSourceSets.map { it.name }.toSet(),
        )
    }

    @Test
    fun `GIVEN the intermediate task WHEN reading its output THEN it lives under generated fakt synthetic`() {
        val project = sharedProject()

        val output = project.sharedTask().generatedKotlinDir.get().asFile

        assertTrue(output.path.endsWith(SHARED_OUTPUT), "output: $output")
    }

    @Test
    fun `GIVEN the intermediate task WHEN reading its source set roots THEN every analysed set maps to its directory`() {
        val project = sharedProject()

        val roots = project.sharedTask().sourceSetRoots.get()

        assertEquals(setOf("desktopMain", "desktopAndServerMain", "commonMain"), roots.keys)
        assertEquals(
            listOf(File(project.projectDir, "src/desktopAndServerMain/kotlin").path),
            roots["desktopAndServerMain"],
        )
    }

    @Test
    fun `GIVEN a js target as well WHEN evaluated THEN the intermediate stays synthetic next to the metadata common producer`() {
        val project = sharedProject(withJs = true)

        assertEquals(1, project.tasks.names.count { it == SHARED_TASK })
        assertTrue("faktGenerateMetadataCommonMain" in project.tasks.names)
        assertNull(project.tasks.findByName("faktGenerateCommonMain"))
        assertEquals("desktop", project.sharedTask().context().targetName)
    }

    @Test
    fun `GIVEN an evaluated project WHEN the late pass runs again THEN the test dir registry is unchanged`() {
        val project = sharedProject(withTest = true)
        val before =
            testDirOwners(project).bySourceSet.toMap() to testDirOwners(project).tasks.toSet()

        SyntheticIntermediateWiring.registerMissing(project, project.faktExtension())

        val after =
            testDirOwners(project).bySourceSet.toMap() to testDirOwners(project).tasks.toSet()
        assertEquals(before, after)
        assertEquals(mapOf("desktopAndServerTest" to SHARED_TASK), after.first)
    }

    @Test
    fun `GIVEN a foreign task named faktGenerateDesktopAndServerMain WHEN evaluated THEN the build fails naming the task`() {
        val project = createKmpProject()
        project.getKotlinExtension().apply {
            jvm("desktop")
            jvm("server")
            shareDesktopAndServer(withTest = false)
        }
        project.tasks.register(SHARED_TASK)

        val failure = kotlin.test.assertFails { project.evaluate() }

        val messages = generateSequence<Throwable>(failure) { it.cause }.mapNotNull { it.message }
        assertTrue(
            messages.any { SHARED_TASK in it && "already exists" in it },
            "messages: ${messages.toList()}",
        )
    }

    @Test
    fun `GIVEN edges added after KGP applied the compilations WHEN evaluated THEN the late pass still registers the task`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.getKotlinExtension().apply {
            jvm("desktop")
            jvm("server")
        }
        // Registered after KGP's own hook and before Fakt's late pass.
        project.afterEvaluate { project.getKotlinExtension().shareDesktopAndServer(false) }
        project.pluginManager.apply("com.rsicarelli.fakt")

        project.evaluate()

        assertTrue(SHARED_TASK in project.tasks.names, "tasks: ${project.tasks.names}")
    }

    // ---- B14 ----------------------------------------------------------------------------------

    @Test
    fun `GIVEN a confirmed synthetic owner WHEN asking the gate THEN the target owns the set`() {
        val project = sharedProject()

        assertTrue(syntheticOwnerConfirmed(project, "desktopAndServerMain", "desktop"))
        assertFalse(syntheticOwnerConfirmed(project, "desktopAndServerMain", "server"))
        assertFalse(syntheticOwnerConfirmed(project, "desktopMain", "desktop"), "platform owned")
    }

    @Test
    fun `GIVEN the owner changes after registration WHEN reading the task THEN it has no sources so a set is never generated twice`() {
        val project = createKmpProject()
        project.getKotlinExtension().apply {
            jvm("desktop")
            jvm("server")
            js { nodejs() }
            shareDesktopAndServer(false)
        }
        project.afterEvaluate {
            // A js target now compiles the set too: KGP owns it through a metadata compilation.
            val kotlin = project.getKotlinExtension()
            kotlin.sourceSets
                .getByName("jsMain")
                .dependsOn(kotlin.sourceSets.getByName("desktopAndServerMain"))
        }
        project.evaluate()
        project.plantMarker("desktopAndServerMain", "SharedMarker")

        val task = project.sharedTask()

        assertTrue(task.sources.files.isEmpty(), "sources: ${task.sources.files}")
        assertTrue(task.analysisOnlySources.files.isEmpty())
    }

    @Test
    fun `GIVEN fakt disabled WHEN reading the intermediate task THEN it sees no sources at all`() {
        val project = sharedProject()
        project.plantMarker("desktopAndServerMain", "SharedMarker")
        project.faktExtension().enabled.set(false)

        val task = project.sharedTask()

        assertTrue(task.sources.files.isEmpty(), "sources: ${task.sources.files}")
        assertTrue(task.analysisOnlySources.files.isEmpty())
    }

    // ---- B15 ----------------------------------------------------------------------------------

    @Test
    fun `GIVEN desktopAndServerTest compiled by the test compilations WHEN reading srcDirs THEN only it carries the output through the task`() {
        val project = sharedProject(withTest = true)

        val sharedTest = project.srcDirsOf("desktopAndServerTest")

        assertTrue(sharedTest.hasOutput(SHARED_OUTPUT), "srcDirs: $sharedTest")
        assertTrue(SHARED_TASK in project.depsOf("desktopAndServerTest"))
        assertFalse(project.srcDirsOf("desktopTest").hasOutput(SHARED_OUTPUT))
        assertFalse(project.srcDirsOf("serverTest").hasOutput(SHARED_OUTPUT))
        assertFalse(project.srcDirsOf("commonTest").hasOutput(SHARED_OUTPUT))
    }

    @Test
    fun `GIVEN no desktopAndServerTest WHEN reading srcDirs THEN desktopTest and serverTest each carry the output`() {
        val project = sharedProject(withTest = false)

        listOf("desktopTest", "serverTest").forEach { leaf ->
            assertTrue(project.srcDirsOf(leaf).hasOutput(SHARED_OUTPUT), "$leaf srcDirs")
            assertTrue(SHARED_TASK in project.depsOf(leaf), "$leaf deps: ${project.depsOf(leaf)}")
        }
        assertFalse(project.srcDirsOf("commonTest").hasOutput(SHARED_OUTPUT))
    }

    @Test
    fun `GIVEN the intermediate task WHEN reading the test sets THEN no plain generated dir is registered next to the task output`() {
        val project = sharedProject(withTest = true)

        val plain =
            listOf("desktopAndServerTest", "desktopTest", "serverTest").flatMap { set ->
                project.srcDirsOf(set).filter { it.path.endsWith("generated/fakt/$set/kotlin") }
            }

        assertTrue(plain.isEmpty(), "plain dirs: $plain")
    }

    @Test
    fun `GIVEN an AGP-named lint task WHEN evaluated THEN lint depends on the intermediate task`() {
        val project = createKmpProject()
        project.getKotlinExtension().apply {
            jvm("desktop")
            jvm("server")
            shareDesktopAndServer(false)
        }
        project.tasks.register("lintAnalyzeAndroidHostTest")
        project.evaluate()

        val deps =
            project.tasks
                .getByName("lintAnalyzeAndroidHostTest")
                .taskDependencies
                .getDependencies(null)
        assertTrue(deps.any { it.name == SHARED_TASK }, "deps: ${deps.map { it.name }}")
    }

    // ---- B16 ----------------------------------------------------------------------------------

    @Test
    fun `GIVEN an all-jvm project with an intermediate WHEN evaluated THEN the commonMain synthetic producer is unchanged`() {
        val project = sharedProject()

        val common = project.tasks.getByName("faktGenerateCommonMain") as FaktGenerateTask
        assertEquals(
            "faktGenerateCommonMain",
            project.extensions.extraProperties.get(COMMON_TEST_OWNER_PROPERTY),
        )
        assertEquals(mapOf("commonMain" to "fakt://generated"), common.context().outputDirectories)
        assertTrue(
            common.generatedKotlinDir.get().asFile.path.endsWith(CANONICAL_COMMON_TEST_DIR),
            "output: ${common.generatedKotlinDir.get()}",
        )
        assertTrue(project.srcDirsOf("commonTest").hasOutput(CANONICAL_COMMON_TEST_DIR))
        assertTrue("faktGenerateCommonMain" in project.depsOf("commonTest"))
    }

    // ---- unsupported shapes -------------------------------------------------------------------

    @Test
    fun `GIVEN an intermediate shared by two js targets only WHEN evaluated THEN no synthetic task exists for it`() {
        val project = createKmpProject()
        project.getKotlinExtension().apply {
            js("browser") { nodejs() }
            js("legacy") { nodejs() }
            val shared = sourceSets.create("allJsMain")
            shared.dependsOn(sourceSets.getByName("commonMain"))
            sourceSets.getByName("browserMain").dependsOn(shared)
            sourceSets.getByName("legacyMain").dependsOn(shared)
        }
        project.evaluate()

        assertNull(project.tasks.findByName("faktGenerateAllJsMain"))
    }

    @Test
    fun `GIVEN an intermediate shared by two js targets only WHEN evaluated THEN a warning names the set`() {
        val project = createKmpProject()
        project.getKotlinExtension().apply {
            js("browser") { nodejs() }
            js("legacy") { nodejs() }
            val shared = sourceSets.create("allJsMain")
            shared.dependsOn(sourceSets.getByName("commonMain"))
            sourceSets.getByName("browserMain").dependsOn(shared)
            sourceSets.getByName("legacyMain").dependsOn(shared)
        }
        project.evaluate()

        val warnings = mutableListOf<String>()
        SyntheticIntermediateWiring.registerMissing(project, project.faktExtension(), warnings::add)

        assertEquals(1, warnings.size, "warnings: $warnings")
        assertTrue("'allJsMain'" in warnings.single(), warnings.single())
    }

    @Test
    fun `GIVEN a confirmed synthetic owner with a non-jvm representative WHEN describing it THEN the message names the set and says it is unsupported`() {
        val message =
            unsupportedSyntheticOwnerMessage(
                SourceSetOwner.Synthetic("allJsMain", "browser", "browserMain"),
                "js",
            )

        assertTrue("allJsMain" in message && "browser" in message, message)
        assertTrue("not generated" in message, message)
    }
}
