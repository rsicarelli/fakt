// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinHierarchyTemplate
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCommonCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val EDGE_LOG = "fakt.contract.edgeLog"

/** Edges seen per `target/compilation` label. */
internal class EdgeLog {
    val byLabel: MutableMap<String, Set<String>> = linkedMapOf()
}

internal fun Project.edgeLog(): MutableMap<String, Set<String>> {
    val extra = extensions.extraProperties
    if (!extra.has(EDGE_LOG)) extra.set(EDGE_LOG, EdgeLog())
    return (extra.get(EDGE_LOG) as EdgeLog).byLabel
}

internal fun Iterable<KotlinSourceSet>.edgeSnapshot(): Set<String> =
    flatMap { child -> child.dependsOn.map { parent -> "${child.name}->${parent.name}" } }.toSet()

/** Records the `dependsOn` edges (as `child->parent`) it sees on every `applyToCompilation`. */
internal class EdgeProbePlugin : KotlinCompilerPluginSupportPlugin {
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "fakt.contract.probe"

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact("com.rsicarelli.fakt", "probe-never-resolved", "0")

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val kotlin = project.extensions.getByType(KotlinMultiplatformExtension::class.java)
        val label = "${kotlinCompilation.target.name}/${kotlinCompilation.name}"
        project.edgeLog()[label] = kotlin.sourceSets.edgeSnapshot()
        return project.provider { emptyList() }
    }
}

/**
 * Pins what Fakt's intermediate source set support (#162) relies on from the Kotlin Gradle Plugin.
 * These tests contain no Fakt code: they apply only KGP (and, for the timing cases, a probe
 * subplugin) so a KGP release that changes any of these facts fails here first.
 *
 * Facts pinned:
 * - which metadata compilations KGP builds for `webMain` (js + wasmJs) and for an all-JVM
 *   intermediate source set;
 * - which `dependsOn` edges are visible when KGP calls `applyToCompilation` for a platform `main`
 *   compilation, for each way an intermediate source set can come into being.
 */
@OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KotlinGradlePluginIntermediateContractTest {

    private enum class ProbeOrder {
        PROBE_AFTER_KGP,
        PROBE_BEFORE_KGP,
    }

    private fun kmpProjectWithProbe(order: ProbeOrder): Project =
        ProjectBuilder.builder().build().also { project ->
            if (order == ProbeOrder.PROBE_BEFORE_KGP) {
                project.pluginManager.apply(EdgeProbePlugin::class.java)
            }
            project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
            if (order == ProbeOrder.PROBE_AFTER_KGP) {
                project.pluginManager.apply(EdgeProbePlugin::class.java)
            }
        }

    private fun bareKmpProject(): Project =
        ProjectBuilder.builder().build().also {
            it.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        }

    private fun Project.declareJsAndWasm() {
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
        }
    }

    private fun Project.metadataCompilations(): Map<String, KotlinCompilation<*>> =
        getKotlinExtension()
            .targets
            .filterIsInstance<KotlinMetadataTarget>()
            .single()
            .compilations
            .associateBy { it.name }

    private val desktopAndServerEdges =
        setOf(
            "desktopAndServerMain->commonMain",
            "desktopMain->desktopAndServerMain",
            "serverMain->desktopAndServerMain",
        )

    // ---- B4: metadata compilations ------------------------------------------------------------

    @Test
    fun `GIVEN js and wasmJs targets WHEN evaluating THEN KGP builds a common webMain metadata compilation`() {
        val project = bareKmpProject().also { it.declareJsAndWasm() }

        project.evaluate()

        val webMain = assertNotNull(project.metadataCompilations()["webMain"])
        assertIs<KotlinCommonCompilation>(webMain)
        assertFalse(webMain is KotlinNativeCompilation, "webMain must not be a native compilation")
        assertEquals("webMain", webMain.defaultSourceSet.name)
        assertEquals(
            setOf("commonMain"),
            webMain.defaultSourceSet.dependsOn.map { it.name }.toSet(),
            "webMain must refine commonMain",
        )
    }

    @Test
    fun `GIVEN js and wasmJs targets WHEN evaluating THEN webMain depends on the commonMain metadata compilation task`() {
        val project = bareKmpProject().also { it.declareJsAndWasm() }
        project.evaluate()
        val compilations = project.metadataCompilations()
        val webMain = assertNotNull(compilations["webMain"])
        val commonMain = assertNotNull(compilations["commonMain"])

        val taskDependencies =
            webMain.compileDependencyFiles.buildDependencies.getDependencies(null).map { it.name }

        assertTrue(
            commonMain.compileTaskProvider.name in taskDependencies,
            "webMain compileDependencyFiles must be built by ${commonMain.compileTaskProvider.name};" +
                " got $taskDependencies",
        )
        assertEquals(
            setOf("commonMain", "webMain"),
            compilations.keys - "main",
            "metadata compilations: ${compilations.keys}",
        )
    }

    @Test
    fun `GIVEN desktop and server jvm targets with desktopAndServerMain WHEN evaluating THEN KGP builds no compilation for it`() {
        val project =
            bareKmpProject().also { project ->
                project.declareDesktopAndServer()
                project.getKotlinExtension().applyDefaultHierarchyTemplate {
                    common {
                        group("desktopAndServer") {
                            withCompilations { it.target.name in setOf("desktop", "server") }
                        }
                    }
                }
            }

        project.evaluate()

        assertEquals(setOf("main"), project.metadataCompilations().keys)
        assertNotNull(project.getKotlinExtension().sourceSets.findByName("desktopAndServerMain"))
    }

    // ---- B5: edge visibility when applyToCompilation fires ------------------------------------

    @Test
    fun `GIVEN user dependsOn edges WHEN KGP calls applyToCompilation THEN the edges are visible in either plugin order`() {
        ProbeOrder.entries.forEach { order ->
            val project = kmpProjectWithProbe(order).also { it.declareDesktopAndServer() }
            project.getKotlinExtension().sourceSets.apply {
                val mid = create("desktopAndServerMain") { it.dependsOn(getByName("commonMain")) }
                getByName("desktopMain").dependsOn(mid)
                getByName("serverMain").dependsOn(mid)
            }

            project.evaluate()

            val seen = assertNotNull(project.edgeLog()["desktop/main"], "order $order")
            assertTrue(seen.containsAll(desktopAndServerEdges), "order $order: $seen")
        }
    }

    @Test
    fun `GIVEN an explicit default hierarchy template group WHEN KGP calls applyToCompilation THEN the group edges are visible`() {
        ProbeOrder.entries.forEach { order ->
            val project = kmpProjectWithProbe(order).also { it.declareDesktopAndServer() }
            project.getKotlinExtension().applyDefaultHierarchyTemplate {
                common {
                    group("desktopAndServer") {
                        withCompilations { it.target.name in setOf("desktop", "server") }
                    }
                }
            }

            project.evaluate()

            val seen = assertNotNull(project.edgeLog()["desktop/main"], "order $order")
            assertTrue(seen.containsAll(desktopAndServerEdges), "order $order: $seen")
        }
    }

    @Test
    fun `GIVEN applyHierarchyTemplate with a group WHEN KGP calls applyToCompilation THEN the group edges are visible`() {
        ProbeOrder.entries.forEach { order ->
            val project = kmpProjectWithProbe(order).also { it.declareDesktopAndServer() }
            project
                .getKotlinExtension()
                .applyHierarchyTemplate(
                    KotlinHierarchyTemplate {
                        withSourceSetTree(KotlinSourceSetTree.main, KotlinSourceSetTree.test)
                        common {
                            withCompilations { true }
                            group("desktopAndServer") {
                                withCompilations { it.target.name in setOf("desktop", "server") }
                            }
                        }
                    }
                )

            project.evaluate()

            val seen = assertNotNull(project.edgeLog()["desktop/main"], "order $order")
            assertTrue(seen.containsAll(desktopAndServerEdges), "order $order: $seen")
        }
    }

    @Test
    fun `GIVEN an explicit applyDefaultHierarchyTemplate with js and wasmJs WHEN KGP calls applyToCompilation THEN the webMain edges are visible`() {
        ProbeOrder.entries.forEach { order ->
            val project = kmpProjectWithProbe(order).also { it.declareJsAndWasm() }
            project.getKotlinExtension().applyDefaultHierarchyTemplate()

            project.evaluate()

            val seen = assertNotNull(project.edgeLog()["jvm/main"], "order $order")
            assertTrue(
                setOf("jsMain->webMain", "wasmJsMain->webMain", "webMain->commonMain").all {
                    it in seen
                },
                "order $order: $seen",
            )
        }
    }

    @Test
    fun `GIVEN the implicit default hierarchy WHEN KGP calls applyToCompilation for platform mains THEN webMain does not exist yet`() {
        ProbeOrder.entries.forEach { order ->
            val project = kmpProjectWithProbe(order).also { it.declareJsAndWasm() }

            project.evaluate()

            val kotlin = project.getKotlinExtension()
            listOf("jvm/main", "js/main", "wasmJs/main").forEach { label ->
                val seen = assertNotNull(project.edgeLog()[label], "order $order, $label")
                assertTrue(
                    seen.none { "webMain" in it },
                    "order $order: the implicit default hierarchy is applied after $label: $seen",
                )
            }
            val finalEdges = kotlin.sourceSets.edgeSnapshot()
            assertTrue("jsMain->webMain" in finalEdges, "after evaluate: $finalEdges")
            assertTrue("webMain->commonMain" in finalEdges, "after evaluate: $finalEdges")
        }
    }

    @Test
    fun `GIVEN the implicit default hierarchy WHEN KGP calls applyToCompilation for the webMain metadata compilation THEN the edges are visible`() {
        val project = kmpProjectWithProbe(ProbeOrder.PROBE_AFTER_KGP).also { it.declareJsAndWasm() }

        project.evaluate()

        val seen = assertNotNull(project.edgeLog()["metadata/webMain"])
        assertTrue("webMain->commonMain" in seen, "$seen")
        assertTrue("jsMain->webMain" in seen, "$seen")
    }
}
