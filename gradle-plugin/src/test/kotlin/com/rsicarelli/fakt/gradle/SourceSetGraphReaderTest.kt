// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.junit.jupiter.api.TestInstance

@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SourceSetGraphReaderTest {
    private fun kmpProject(): Project =
        ProjectBuilder.builder().build().also {
            it.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        }

    private fun Project.declareJvmJsWasm() {
        getKotlinExtension().apply {
            jvm()
            js { nodejs() }
            wasmJs { nodejs() }
        }
    }

    private fun Project.declareDesktopAndServer() {
        getKotlinExtension().apply {
            jvm("desktop")
            jvm("server")
            applyDefaultHierarchyTemplate {
                common {
                    group("desktopAndServer") {
                        withCompilations { it.target.name in setOf("desktop", "server") }
                    }
                }
            }
        }
    }

    @Test
    fun `GIVEN jvm js and wasmJs WHEN reading the graph THEN targets exclude the metadata target and carry main source sets`() {
        val project = kmpProject().also { it.declareJvmJsWasm() }
        project.evaluate()

        val graph = readSourceSetGraph(project.getKotlinExtension())

        assertEquals(
            mapOf("jvm" to "jvm", "js" to "js", "wasmJs" to "wasm"),
            graph.targets.associate { it.name to it.platformType },
        )
        assertEquals(listOf("jvmMain"), graph.targets.single { it.name == "jvm" }.mainSourceSets)
    }

    @Test
    fun `GIVEN jvm js and wasmJs WHEN reading the graph THEN parents hold the dependsOn edges and commonMain above every main set`() {
        val project = kmpProject().also { it.declareJvmJsWasm() }
        project.evaluate()

        val graph = readSourceSetGraph(project.getKotlinExtension())

        assertTrue("webMain" in graph.parents.getValue("jsMain"))
        assertTrue("webMain" in graph.parents.getValue("wasmJsMain"))
        assertTrue("commonMain" in graph.parents.getValue("webMain"))
        assertTrue("commonMain" in graph.parents.getValue("jvmMain"))
        assertFalse(
            assignSourceSetOwners(graph).keys.any { it.endsWith("Test") },
            "no test set is owned: ${graph.targets}",
        )
    }

    @Test
    fun `GIVEN jvm js and wasmJs WHEN reading the graph THEN the non main metadata compilations are listed`() {
        val project = kmpProject().also { it.declareJvmJsWasm() }
        project.evaluate()

        val graph = readSourceSetGraph(project.getKotlinExtension())

        assertEquals(setOf("commonMain", "webMain"), graph.metadataCompilations)
        assertEquals(SourceSetOwner.Metadata("webMain"), assignSourceSetOwners(graph)["webMain"])
    }

    @Test
    fun `GIVEN desktop and server with desktopAndServerMain WHEN reading the graph THEN no metadata compilation is listed and the set is synthetic`() {
        val project = kmpProject().also { it.declareDesktopAndServer() }
        project.evaluate()

        val graph = readSourceSetGraph(project.getKotlinExtension())
        val owners = assignSourceSetOwners(graph)

        assertEquals(emptySet(), graph.metadataCompilations)
        assertEquals(
            SourceSetOwner.Synthetic("desktopAndServerMain", "desktop", "desktopMain"),
            owners["desktopAndServerMain"],
        )
        assertEquals(
            SourceSetOwner.Synthetic("commonMain", "desktop", "desktopMain"),
            owners["commonMain"],
        )
    }

    @Test
    fun `GIVEN a custom compilation associated with main WHEN reading the graph THEN its source set is not a main source set and owns nothing`() {
        val project = kmpProject().also { it.declareDesktopAndServer() }
        val kmp = project.getKotlinExtension()
        val server = kmp.targets.getByName("server") as KotlinJvmTarget
        val integration = server.compilations.create("integration")
        integration.associateWith(server.compilations.getByName("main"))
        kmp.sourceSets
            .getByName("serverIntegration")
            .dependsOn(kmp.sourceSets.getByName("commonTest"))
        project.evaluate()

        val graph = readSourceSetGraph(kmp)
        val owners = assignSourceSetOwners(graph)

        assertEquals(
            listOf("serverMain"),
            graph.targets.single { it.name == "server" }.mainSourceSets,
        )
        assertFalse("serverIntegration" in owners, "no owner for the test-like set: $owners")
        assertFalse("commonTest" in owners, "no commonTest owner: $owners")
    }

    @Test
    fun `GIVEN a custom compilation without test in its name and no association WHEN reading the graph THEN it stays a main compilation`() {
        val project = kmpProject().also { it.declareDesktopAndServer() }
        val kmp = project.getKotlinExtension()
        val server = kmp.targets.getByName("server") as KotlinJvmTarget
        server.compilations.create("benchmark")
        project.evaluate()

        val graph = readSourceSetGraph(kmp)

        assertEquals(
            setOf("serverMain", "serverBenchmark"),
            graph.targets.single { it.name == "server" }.mainSourceSets.toSet(),
        )
    }
}
