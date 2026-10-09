// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import com.rsicarelli.fakt.gradle.helpers.kmpCompilation
import java.io.File
import kotlin.test.assertEquals
import org.gradle.api.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins what a non-Android consumer task is fed and what its serialized source set context holds, so
 * the per-variant consumer split (#163) provably leaves every other target byte-identical.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktConsumerSourceSplitTest {

    private fun jvmJsConsumer(): Project =
        createKmpProject()
            .also {
                it.getKotlinExtension().jvm()
                it.getKotlinExtension().js()
                it.evaluate()
            }
            .also { project ->
                FaktGenerateTaskWiring.registerConsumer(
                    project,
                    project.kmpCompilation("jvm", "main"),
                    project.extensions.getByType(FaktPluginExtension::class.java),
                )
            }

    private fun Project.plantMarker(sourceSet: String) {
        File(projectDir, "src/$sourceSet/kotlin")
            .apply { mkdirs() }
            .resolve("Marker.kt")
            .writeText("// marker\n")
    }

    private fun Project.relative(files: Iterable<File>): List<String> =
        files.map { it.relativeTo(projectDir).path }.sorted()

    @Test
    fun `GIVEN a jvm and js project WHEN reading the jvm consumer THEN own is jvmMain and analysis only is commonMain`() {
        val project = jvmJsConsumer()
        listOf("jvmMain", "commonMain", "jsMain").forEach { project.plantMarker(it) }
        val task = project.tasks.getByName("faktGenerateJvmMain") as FaktGenerateTask

        assertEquals(listOf("src/jvmMain/kotlin/Marker.kt"), project.relative(task.sources.files))
        assertEquals(
            listOf("src/commonMain/kotlin/Marker.kt"),
            project.relative(task.analysisOnlySources.files),
        )
    }

    @Test
    fun `GIVEN a jvm and js project WHEN encoding the jvm consumer context THEN it equals the golden`() {
        val project = jvmJsConsumer()
        val task = project.tasks.getByName("faktGenerateJvmMain") as FaktGenerateTask

        assertEquals(GOLDEN_JVM_CONSUMER_CONTEXT, task.sourceSetContextJson.get())
    }

    private companion object {
        const val GOLDEN_JVM_CONSUMER_CONTEXT =
            """{"compilationName":"main","targetName":"jvm","platformType":"jvm","isTest":false,"defaultSourceSet":{"name":"jvmMain","parents":["commonMain"]},"allSourceSets":[{"name":"jvmMain","parents":["commonMain"]},{"name":"commonMain","parents":[]}],"outputDirectory":"fakt://generated","commonTestOutputDirectory":"fakt://generated","outputDirectories":{"jvmMain":"fakt://generated"}}"""
    }
}
