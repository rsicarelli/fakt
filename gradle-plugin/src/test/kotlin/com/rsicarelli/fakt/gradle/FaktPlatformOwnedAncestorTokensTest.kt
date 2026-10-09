// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins B17 of #162: a consumer also emits every ancestor that only its own target compiles. With
 * jvm + js and `jvmMain` depending on a hand-written `sharedJvmMain`, nobody else would generate
 * `sharedJvmMain`'s fakes: not the metadata producer (js never compiles it), not the js consumer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktPlatformOwnedAncestorTokensTest {

    private fun consumerContext(): SourceSetContext {
        val common = SourceSetInfo("commonMain", parents = emptyList())
        val shared = SourceSetInfo("sharedJvmMain", parents = listOf("commonMain"))
        val jvm = SourceSetInfo("jvmMain", parents = listOf("sharedJvmMain"))
        return SourceSetContext(
            compilationName = "main",
            targetName = "jvm",
            platformType = "jvm",
            isTest = false,
            defaultSourceSet = jvm,
            allSourceSets = listOf(jvm, shared, common),
            outputDirectory = "fakt://generated",
            commonTestOutputDirectory = "fakt://generated",
        )
    }

    @Test
    fun `GIVEN platform-owned ancestors WHEN building consumer tokens THEN the default set and every ancestor map to the generated token`() {
        val tokens =
            outputRouteTokens(
                consumerContext(),
                TaskShape.CONSUMER,
                platformOwned = setOf("sharedJvmMain"),
            )

        assertEquals(
            mapOf("jvmMain" to "fakt://generated", "sharedJvmMain" to "fakt://generated"),
            tokens,
        )
    }

    @Test
    fun `GIVEN no platform-owned ancestors WHEN building consumer tokens THEN only the default set is routed`() {
        val tokens = outputRouteTokens(consumerContext(), TaskShape.CONSUMER)

        assertEquals(mapOf("jvmMain" to "fakt://generated"), tokens)
    }

    @Test
    fun `GIVEN platform-owned ancestors WHEN building tokens for any other shape THEN they are ignored`() {
        val context = consumerContext()
        val ignored =
            listOf(TaskShape.PRODUCER, TaskShape.SYNTHETIC, TaskShape.INTERMEDIATE_METADATA)

        ignored.forEach { shape ->
            val withOwned =
                outputRouteTokens(context, shape, platformOwned = setOf("sharedJvmMain"))
            assertEquals(outputRouteTokens(context, shape), withOwned, "$shape")
        }
    }

    // ---- ProjectBuilder -----------------------------------------------------------------------

    private fun jvmJsProject(lateEdge: Boolean = false): Project =
        createKmpProject().also { project ->
            val kotlin = project.getKotlinExtension()
            kotlin.jvm()
            kotlin.js { nodejs() }
            val shared = kotlin.sourceSets.create("sharedJvmMain")
            shared.dependsOn(kotlin.sourceSets.getByName("commonMain"))
            if (lateEdge) {
                project.afterEvaluate { kotlin.sourceSets.getByName("jvmMain").dependsOn(shared) }
            } else {
                kotlin.sourceSets.getByName("jvmMain").dependsOn(shared)
            }
            project.evaluate()
        }

    private fun Project.consumer(name: String): FaktGenerateTask =
        tasks.getByName(name) as FaktGenerateTask

    private fun FaktGenerateTask.tokens(): Map<String, String> =
        Json.decodeFromString(SourceSetContext.serializer(), sourceSetContextJson.get())
            .outputDirectories

    @Test
    fun `GIVEN jvmMain depending on sharedJvmMain in jvm plus js WHEN decoding the jvm consumer THEN both sets are routed`() {
        val project = jvmJsProject()

        val tokens = project.consumer("faktGenerateJvmMain").tokens()

        assertEquals(setOf("jvmMain", "sharedJvmMain"), tokens.keys)
        assertTrue(tokens.values.all { it == "fakt://generated" }, "$tokens")
    }

    @Test
    fun `GIVEN the same project WHEN decoding the js consumer THEN only jsMain is routed`() {
        val project = jvmJsProject()

        assertEquals(setOf("jsMain"), project.consumer("faktGenerateJsMain").tokens().keys)
    }

    @Test
    fun `GIVEN sharedJvmMain sources WHEN reading the jvm consumer THEN they are analysed with it`() {
        val project = jvmJsProject()
        val marker =
            File(project.projectDir, "src/sharedJvmMain/kotlin")
                .apply { mkdirs() }
                .resolve("Shared.kt")
                .apply { writeText("// marker\n") }

        val task = project.consumer("faktGenerateJvmMain")

        assertTrue(marker in task.analysisOnlySources.files, "${task.analysisOnlySources.files}")
        assertTrue(
            task.sourceSetRoots.get().containsKey("sharedJvmMain"),
            "roots: ${task.sourceSetRoots.get()}",
        )
    }

    @Test
    fun `GIVEN the edge to the ancestor is added after the consumer registered WHEN decoding THEN the route is still computed`() {
        val project = jvmJsProject(lateEdge = true)

        val tokens = project.consumer("faktGenerateJvmMain").tokens()

        assertEquals(setOf("jvmMain", "sharedJvmMain"), tokens.keys)
    }

    @Test
    fun `GIVEN a project with only commonMain above the platform sets WHEN decoding a consumer THEN commonMain is never routed`() {
        val project =
            createKmpProject().also {
                it.getKotlinExtension().jvm()
                it.getKotlinExtension().js { nodejs() }
                it.evaluate()
            }

        assertEquals(setOf("jvmMain"), project.consumer("faktGenerateJvmMain").tokens().keys)
    }
}
