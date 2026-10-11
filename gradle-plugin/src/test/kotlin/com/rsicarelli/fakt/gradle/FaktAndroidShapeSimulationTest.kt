// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import com.rsicarelli.fakt.gradle.helpers.kmpCompilation
import java.io.File
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Simulates the Android variant shape (#163) without AGP: a `jvm("android")` target whose main
 * compilation lists an extra source set next to its default one (what KGP does with `androidMain`
 * next to `androidDebug`), and that extra set is not reached from the default through `dependsOn`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktAndroidShapeSimulationTest {

    private fun variantConsumer(): Project =
        createKmpProject().also { project ->
            val kotlin = project.getKotlinExtension()
            kotlin.jvm("android")
            kotlin.js()
            val member = kotlin.sourceSets.create("androidMember")
            project.kmpCompilation("android", "main").addMember(member)
            project.evaluate()
            FaktGenerateTaskWiring.registerConsumer(
                project,
                project.kmpCompilation("android", "main"),
                project.extensions.getByType(FaktPluginExtension::class.java),
            )
        }

    /**
     * KGP's `source(sourceSet)` is internal (AGP's preprocessing calls it for `androidMain`), so
     * the simulation reaches it through the decorated compilation's implementation.
     */
    private fun KotlinCompilation<*>.addMember(sourceSet: KotlinSourceSet) {
        val impl = javaClass.methods.first { it.name.startsWith("getCompilation$") }.invoke(this)
        val container = impl.javaClass.methods.first { it.name == "getSourceSets" }.invoke(impl)
        Class.forName(
                "org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationSourceSetsContainer"
            )
            .methods
            .first { it.name == "source" }
            .invoke(container, sourceSet)
    }

    private fun Project.plantMarker(sourceSet: String) {
        File(projectDir, "src/$sourceSet/kotlin")
            .apply { mkdirs() }
            .resolve("Marker.kt")
            .writeText("// marker\n")
    }

    private fun Project.task(): FaktGenerateTask =
        tasks.getByName("faktGenerateAndroidMain") as FaktGenerateTask

    private fun Project.context(): SourceSetContext =
        Json.decodeFromString(SourceSetContext.serializer(), task().sourceSetContextJson.get())

    @Test
    fun `GIVEN a compilation with a member source set WHEN reading the consumer sources THEN every member is own and only common is analysis only`() {
        val project = variantConsumer()
        listOf("androidMain", "androidMember", "commonMain").forEach { project.plantMarker(it) }

        val own =
            project.task().sources.files.map { it.relativeTo(project.projectDir).path }.sorted()
        val analysis =
            project.task().analysisOnlySources.files.map { it.relativeTo(project.projectDir).path }

        assertEquals(
            listOf("src/androidMain/kotlin/Marker.kt", "src/androidMember/kotlin/Marker.kt"),
            own,
        )
        assertEquals(listOf("src/commonMain/kotlin/Marker.kt"), analysis)
    }

    @Test
    fun `GIVEN a compilation with a member source set WHEN encoding THEN the route tokens emit default and member`() {
        val project = variantConsumer()

        assertEquals(
            setOf("androidMain", "androidMember"),
            project.context().outputDirectories.keys,
        )
    }

    @Test
    fun `GIVEN a member source set outside the default closure WHEN encoding THEN it is appended to allSourceSets`() {
        val project = variantConsumer()
        val member = project.getKotlinExtension().sourceSets.getByName("androidMember")

        val appended = project.context().allSourceSets.last()

        assertEquals("androidMember", appended.name)
        assertEquals(member.dependsOn.map { it.name }.sorted(), appended.parents)
    }
}
