// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the synthetic common producer of [SyntheticProducerWiring]: when every KMP target is
 * JVM-typed, KGP builds no `commonMain` metadata compilation, so one task owns `commonMain`.
 *
 * KGP does call `applyToCompilation` under `ProjectBuilder`, so `evaluate()` fires the hook once
 * per `main` compilation, the way the real build does; the tests rely on that and never call the
 * hook themselves, except the one that checks a second call is harmless.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktSyntheticProducerWiringTest {

    private fun Project.faktExtension(): FaktPluginExtension =
        extensions.getByType(FaktPluginExtension::class.java)

    private fun allJvmProject(): Project =
        createKmpProject().also {
            it.getKotlinExtension().jvm("desktop")
            it.getKotlinExtension().jvm("server")
            it.evaluate()
        }

    private fun Project.plantMarker(sourceSet: String, name: String): File =
        File(projectDir, "src/$sourceSet/kotlin")
            .apply { mkdirs() }
            .resolve("$name.kt")
            .apply { writeText("// marker\n") }

    private fun Project.syntheticTask(): FaktGenerateTask =
        tasks.getByName("faktGenerateCommonMain") as FaktGenerateTask

    private fun Project.syntheticTaskNames(): List<String> =
        tasks.names.filter { it == "faktGenerateCommonMain" }

    @Test
    fun `GIVEN desktop and server jvm targets WHEN the project is evaluated THEN exactly one faktGenerateCommonMain exists`() {
        val project = allJvmProject()

        assertEquals(listOf("faktGenerateCommonMain"), project.syntheticTaskNames())
        val context =
            Json.decodeFromString(
                SourceSetContext.serializer(),
                project.syntheticTask().sourceSetContextJson.get(),
            )
        assertEquals("desktop", context.targetName, "the representative drives the task")
    }

    @Test
    fun `GIVEN the synthetic task WHEN reading its sources THEN commonMain is emitted and only the representative is analysed`() {
        val project = allJvmProject()
        val common = project.plantMarker("commonMain", "CommonMarker")
        val desktop = project.plantMarker("desktopMain", "DesktopMarker")
        val server = project.plantMarker("serverMain", "ServerMarker")

        val task = project.syntheticTask()
        assertTrue(common in task.commonSources.files, "commonSources: ${task.commonSources.files}")
        assertTrue(task.sources.files.isEmpty(), "sources: ${task.sources.files}")
        val analysis = task.platformAnalysisOnlySources.files
        assertTrue(desktop in analysis, "platformAnalysisOnlySources: $analysis")
        assertTrue(server !in analysis, "platformAnalysisOnlySources: $analysis")
        assertTrue(common !in analysis, "platformAnalysisOnlySources: $analysis")
    }

    @Test
    fun `GIVEN fakt disabled WHEN reading the synthetic task THEN it sees no sources at all`() {
        val project = allJvmProject()
        project.plantMarker("commonMain", "CommonMarker")
        project.plantMarker("desktopMain", "DesktopMarker")
        project.faktExtension().enabled.set(false)

        val task = project.syntheticTask()
        assertTrue(task.commonSources.files.isEmpty(), "${task.commonSources.files}")
        assertTrue(task.platformAnalysisOnlySources.files.isEmpty())
    }

    @Test
    fun `GIVEN the synthetic task WHEN reading commonTest THEN its srcDirs carry the task output`() {
        val project = allJvmProject()

        val commonTest = project.getKotlinExtension().sourceSets.getByName("commonTest")
        val deps = commonTest.kotlin.buildDependencies.getDependencies(null).map { it.name }
        assertTrue("faktGenerateCommonMain" in deps, "commonTest deps: $deps")
        assertTrue(
            commonTest.kotlin.srcDirs.any { it.path.endsWith("generated/fakt/commonTest/kotlin") },
            "srcDirs: ${commonTest.kotlin.srcDirs}",
        )
    }

    @Test
    fun `GIVEN the synthetic task WHEN reading the registry THEN it owns commonTest`() {
        val project = allJvmProject()

        assertEquals("faktGenerateCommonMain", testDirOwnerOf(project, "commonTest"))
        assertTrue(isFaktTestDirOwner(project, "faktGenerateCommonMain"))
    }

    @Test
    fun `GIVEN the synthetic task WHEN decoding its context THEN it routes commonMain to the generated token`() {
        val project = allJvmProject()

        val context =
            Json.decodeFromString(
                SourceSetContext.serializer(),
                project.syntheticTask().sourceSetContextJson.get(),
            )
        assertEquals(mapOf("commonMain" to "fakt://generated"), context.outputDirectories)
    }

    @Test
    fun `GIVEN an AGP-named lint task WHEN the project is evaluated THEN lint depends on the synthetic task`() {
        val project = allJvmProject()
        project.tasks.register("lintAnalyzeAndroidHostTest")

        val deps =
            project.tasks
                .getByName("lintAnalyzeAndroidHostTest")
                .taskDependencies
                .getDependencies(null)
        assertTrue(
            deps.any { it.name == "faktGenerateCommonMain" },
            "deps: ${deps.map { it.name }}",
        )
    }

    @Test
    fun `GIVEN an evaluated project WHEN the hook is called again THEN it stays a single task without error`() {
        val project = allJvmProject()

        val main = project.getKotlinExtension().targets.getByName("desktop").compilations
        SyntheticProducerWiring.registerIfRepresentative(
            project,
            main.getByName("main"),
            project.faktExtension(),
        )

        assertEquals(1, project.syntheticTaskNames().size)
    }

    @Test
    fun `GIVEN a foreign task named faktGenerateCommonMain WHEN the project is evaluated THEN it fails loudly`() {
        val project =
            createKmpProject().also {
                it.getKotlinExtension().jvm("desktop")
                it.getKotlinExtension().jvm("server")
                it.tasks.register("faktGenerateCommonMain")
            }

        val failure = assertFails { project.evaluate() }

        val messages = generateSequence<Throwable>(failure) { it.cause }.map { it.message }.toList()
        assertTrue(
            messages.any { it.orEmpty().contains("a task with that name already exists") },
            "messages: $messages",
        )
    }

    @Test
    fun `GIVEN source sets without commonMain WHEN picking the common one THEN the error names it`() {
        val project = allJvmProject()
        val lonely = project.getKotlinExtension().sourceSets.create("lonelyMain")

        val failure =
            assertFailsWith<IllegalStateException> { requireCommonMain(listOf(lonely), "desktop") }

        assertTrue(failure.message.orEmpty().contains("commonMain"), "${failure.message}")
        assertTrue(failure.message.orEmpty().contains("lonelyMain"), "${failure.message}")
        assertTrue(failure.message.orEmpty().contains("desktop"), "${failure.message}")
    }

    @Test
    fun `GIVEN a user FaktGenerateTask named faktGenerateCommonMain WHEN evaluated THEN it fails loudly`() {
        val project =
            createKmpProject().also {
                it.getKotlinExtension().jvm("desktop")
                it.getKotlinExtension().jvm("server")
                it.tasks.register("faktGenerateCommonMain", FaktGenerateTask::class.java)
            }

        val failure = assertFails { project.evaluate() }

        val messages = generateSequence<Throwable>(failure) { it.cause }.map { it.message }.toList()
        assertTrue(
            messages.any {
                it.orEmpty().contains("task with that name already exists") &&
                    it.orEmpty().contains("faktGenerateCommonMain")
            },
            "messages: $messages",
        )
    }

    @Test
    fun `GIVEN jvm and js targets WHEN the project is evaluated THEN no synthetic task is registered`() {
        val project =
            createKmpProject().also {
                it.getKotlinExtension().jvm()
                it.getKotlinExtension().js { nodejs() }
                it.evaluate()
            }

        assertNull(project.tasks.findByName("faktGenerateCommonMain"))
    }

    @Test
    fun `GIVEN jvm and linuxX64 targets WHEN the project is evaluated THEN no synthetic task is registered`() {
        val project =
            createKmpProject().also {
                it.getKotlinExtension().jvm()
                it.getKotlinExtension().linuxX64()
                it.evaluate()
            }

        assertNull(project.tasks.findByName("faktGenerateCommonMain"))
    }

    @Test
    fun `GIVEN a single jvm target WHEN the project is evaluated THEN no synthetic task is registered`() {
        val project =
            createKmpProject().also {
                it.getKotlinExtension().jvm()
                it.evaluate()
            }

        assertNull(project.tasks.findByName("faktGenerateCommonMain"))
    }

    @Test
    fun `GIVEN two jvm targets WHEN asking KGP THEN the metadata target has no commonMain compilation`() {
        val project = allJvmProject()

        val metadata =
            project.getKotlinExtension().targets.withType(KotlinMetadataTarget::class.java)

        assertNull(
            metadata.firstOrNull()?.compilations?.findByName("commonMain"),
            "KGP contract: no per-source-set commonMain compilation when every target is jvm",
        )
    }

    @Test
    fun `GIVEN jvm and js targets WHEN asking KGP THEN the metadata target has a commonMain compilation`() {
        val project =
            createKmpProject().also {
                it.getKotlinExtension().jvm()
                it.getKotlinExtension().js { nodejs() }
                it.evaluate()
            }

        val metadata =
            project.getKotlinExtension().targets.withType(KotlinMetadataTarget::class.java)

        assertNotNull(
            metadata.firstOrNull()?.compilations?.findByName("commonMain"),
            "KGP contract: a commonMain compilation exists when the platform types differ",
        )
    }

    @Test
    fun `GIVEN target shapes WHEN predicting the synthetic representative THEN only same-type non-native multi-target wins`() {
        fun node(name: String, type: String, android: Boolean = false) =
            TargetNode(name, type, android, listOf("${name}Main"))

        assertEquals(
            "desktop",
            predictSyntheticCommonMainTarget(listOf(node("server", "jvm"), node("desktop", "jvm")))
                ?.name,
        )
        assertEquals(
            "jvm",
            predictSyntheticCommonMainTarget(
                    listOf(node("android", "jvm", android = true), node("jvm", "jvm"))
                )
                ?.name,
        )
        assertNull(predictSyntheticCommonMainTarget(listOf(node("jvm", "jvm"))))
        assertNull(predictSyntheticCommonMainTarget(listOf(node("jvm", "jvm"), node("js", "js"))))
        assertNull(
            predictSyntheticCommonMainTarget(listOf(node("a", "native"), node("b", "native")))
        )
    }
}
