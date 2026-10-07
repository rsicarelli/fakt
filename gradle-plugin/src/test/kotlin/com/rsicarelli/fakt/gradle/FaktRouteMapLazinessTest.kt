// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import com.rsicarelli.fakt.gradle.helpers.kmpCompilation
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * The route map names source sets, and a source set missing from it is not emitted. So the map must
 * be built when the task runs, after KGP's `dependsOn` edges are final, not when it is registered.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktRouteMapLazinessTest {

    private fun decode(project: Project, taskName: String): SourceSetContext {
        val task = project.tasks.getByName(taskName) as FaktGenerateTask
        return Json.decodeFromString(SourceSetContext.serializer(), task.sourceSetContextJson.get())
    }

    @Test
    fun `GIVEN a single-target task WHEN an intermediate source set is added after registration THEN the route map lists it`() {
        val project = createKmpProject().also { it.getKotlinExtension().jvm() }
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)
        FaktGenerateTaskWiring.registerSingleTarget(
            project,
            project.kmpCompilation("jvm", "main"),
            extension,
        )

        val sourceSets = project.getKotlinExtension().sourceSets
        val shared = sourceSets.create("jvmShared")
        sourceSets.getByName("jvmMain").dependsOn(shared)
        shared.dependsOn(sourceSets.getByName("commonMain"))
        project.evaluate()

        val routes = decode(project, "faktGenerateJvmMain").outputDirectories
        assertEquals("fakt://common", routes["jvmShared"], "routes: $routes")
    }

    @Test
    fun `GIVEN a registered task WHEN reading the context THEN it carries no absolute path`() {
        val project =
            createKmpProject().also {
                it.getKotlinExtension().jvm()
                it.evaluate()
            }
        FaktGenerateTaskWiring.registerSingleTarget(
            project,
            project.kmpCompilation("jvm", "main"),
            project.extensions.getByType(FaktPluginExtension::class.java),
        )

        val json =
            (project.tasks.getByName("faktGenerateJvmMain") as FaktGenerateTask)
                .sourceSetContextJson
                .get()

        assertFalse(json.contains(project.projectDir.absolutePath), json)
    }
}
