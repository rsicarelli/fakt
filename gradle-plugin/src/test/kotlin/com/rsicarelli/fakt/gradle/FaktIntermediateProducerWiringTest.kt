// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

private const val WEB_TASK = "faktGenerateMetadataWebMain"
private const val WEB_OUTPUT = "generated/fakt/metadata/webMain/kotlin"

/**
 * Pins the producer of a metadata intermediate source set (#162): with js + wasmJs, KGP builds a
 * `webMain` metadata compilation, and one `faktGenerateMetadataWebMain` task owns the `webMain`
 * fakes. It analyses `webMain` only: `commonMain` reaches it as the compiled metadata klib on the
 * classpath and as the `-Xrefines-paths` input, never as sources.
 *
 * KGP calls `applyToCompilation` under `ProjectBuilder`, so `evaluate()` registers the tasks the
 * way a real build does.
 */
@OptIn(ExperimentalWasmDsl::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktIntermediateProducerWiringTest {

    private fun Project.faktExtension(): FaktPluginExtension =
        extensions.getByType(FaktPluginExtension::class.java)

    private fun webProject(): Project =
        createKmpProject().also {
            it.getKotlinExtension().apply {
                js { nodejs() }
                wasmJs { nodejs() }
            }
            it.evaluate()
        }

    /** js + wasmJs with a hand-written `webMain` and no `webTest`: the 2.2.0 style. */
    private fun manualWebProject(): Project =
        createKmpProject().also {
            it.getKotlinExtension().apply {
                js { nodejs() }
                wasmJs { nodejs() }
                val webMain = sourceSets.create("webMain")
                webMain.dependsOn(sourceSets.getByName("commonMain"))
                sourceSets.getByName("jsMain").dependsOn(webMain)
                sourceSets.getByName("wasmJsMain").dependsOn(webMain)
            }
            it.evaluate()
        }

    private fun Project.plantMarker(sourceSet: String, name: String): File =
        File(projectDir, "src/$sourceSet/kotlin")
            .apply { mkdirs() }
            .resolve("$name.kt")
            .apply { writeText("// marker\n") }

    private fun Project.webTask(): FaktGenerateTask = tasks.getByName(WEB_TASK) as FaktGenerateTask

    private fun Project.metadataCompileTaskName(compilation: String): String =
        getKotlinExtension()
            .targets
            .filterIsInstance<KotlinMetadataTarget>()
            .single()
            .compilations
            .getByName(compilation)
            .compileTaskProvider
            .name

    private fun FaktGenerateTask.context(): SourceSetContext =
        Json.decodeFromString(SourceSetContext.serializer(), sourceSetContextJson.get())

    private fun Project.srcDirsOf(sourceSet: String): Set<File> =
        getKotlinExtension().sourceSets.getByName(sourceSet).kotlin.srcDirs

    private fun Set<File>.hasOutput(path: String): Boolean = any { it.path.endsWith(path) }

    // ---- B7 -----------------------------------------------------------------------------------

    @Test
    fun `GIVEN js and wasmJs targets WHEN the project is evaluated THEN exactly one faktGenerateMetadataWebMain exists next to the common producer`() {
        val project = webProject()

        val names = project.tasks.names.filter { it.startsWith("faktGenerateMetadata") }.sorted()

        assertEquals(listOf("faktGenerateMetadataCommonMain", WEB_TASK), names)
    }

    @Test
    fun `GIVEN webMain and commonMain sources WHEN reading the webMain producer THEN it sees webMain only`() {
        val project = webProject()
        val web = project.plantMarker("webMain", "WebMarker")
        val common = project.plantMarker("commonMain", "CommonMarker")

        val task = project.webTask()

        assertEquals(setOf(web), task.sources.files, "sources")
        assertTrue(common !in task.analysisOnlySources.files, "analysisOnlySources")
        assertTrue(task.commonSources.files.isEmpty(), "commonSources")
        assertTrue(task.platformAnalysisOnlySources.files.isEmpty(), "platformAnalysisOnlySources")
    }

    @Test
    fun `GIVEN the webMain producer WHEN reading its klib inputs THEN both are built by the commonMain metadata compilation`() {
        val project = webProject()
        val commonCompile = project.metadataCompileTaskName("commonMain")
        val task = project.webTask()

        val classpathDeps =
            task.commonKlibClasspath.buildDependencies.getDependencies(null).map { it.name }
        val refinesDeps = task.refinesKlibs.buildDependencies.getDependencies(null).map { it.name }

        assertTrue(commonCompile in classpathDeps, "commonKlibClasspath deps: $classpathDeps")
        assertTrue(commonCompile in refinesDeps, "refinesKlibs deps: $refinesDeps")
        assertTrue(task.compileClasspath.isEmpty, "klib compilations use commonKlibClasspath only")
    }

    @Test
    fun `GIVEN the webMain producer WHEN decoding its context THEN it routes webMain to the generated token`() {
        val project = webProject()

        val context = project.webTask().context()

        assertEquals(mapOf("webMain" to "fakt://generated"), context.outputDirectories)
        assertEquals("webMain", context.defaultSourceSet.name)
        assertEquals("common", context.platformType.lowercase())
    }

    @Test
    fun `GIVEN the webMain producer WHEN reading its output THEN it lives under generated fakt metadata webMain`() {
        val project = webProject()

        val output = project.webTask().generatedKotlinDir.get().asFile

        assertTrue(output.path.endsWith(WEB_OUTPUT), "output: $output")
    }

    @Test
    fun `GIVEN the webMain producer WHEN reading its source set roots THEN every analysed set maps to its source directory`() {
        val project = webProject()

        val roots = project.webTask().sourceSetRoots.get()

        assertEquals(
            listOf(File(project.projectDir, "src/webMain/kotlin").path),
            roots["webMain"],
            "roots: $roots",
        )
        assertEquals(
            listOf(File(project.projectDir, "src/commonMain/kotlin").path),
            roots["commonMain"],
            "roots: $roots",
        )
    }

    // ---- source set roots on the other shapes -------------------------------------------------

    @Test
    fun `GIVEN a js consumer under webMain WHEN reading its source set roots THEN jsMain webMain and commonMain are mapped`() {
        val project = webProject()

        val consumer = project.tasks.getByName("faktGenerateJsMain") as FaktGenerateTask
        val roots = consumer.sourceSetRoots.get()

        assertEquals(setOf("jsMain", "webMain", "commonMain"), roots.keys)
        assertEquals(listOf(File(project.projectDir, "src/webMain/kotlin").path), roots["webMain"])
    }

    @Test
    fun `GIVEN the commonMain producer WHEN reading its source set roots THEN commonMain is mapped`() {
        val project = webProject()

        val producer = project.tasks.getByName("faktGenerateMetadataCommonMain") as FaktGenerateTask

        assertEquals(
            listOf(File(project.projectDir, "src/commonMain/kotlin").path),
            producer.sourceSetRoots.get()["commonMain"],
        )
    }

    // ---- B8 -----------------------------------------------------------------------------------

    @Test
    fun `GIVEN webTest compiled by the test compilations WHEN reading its srcDirs THEN it carries the producer output through the task`() {
        val project = webProject()

        val webTest = project.getKotlinExtension().sourceSets.getByName("webTest")
        val deps = webTest.kotlin.buildDependencies.getDependencies(null).map { it.name }

        assertTrue(
            webTest.kotlin.srcDirs.hasOutput(WEB_OUTPUT),
            "srcDirs: ${webTest.kotlin.srcDirs}",
        )
        assertTrue(WEB_TASK in deps, "webTest deps: $deps")
        assertTrue(!project.srcDirsOf("jsTest").hasOutput(WEB_OUTPUT), "jsTest sees it via webTest")
        assertTrue(!project.srcDirsOf("wasmJsTest").hasOutput(WEB_OUTPUT), "wasmJsTest too")
    }

    @Test
    fun `GIVEN no webTest in any test compilation WHEN reading the leaf test source sets THEN each of jsTest and wasmJsTest gets the output`() {
        val project = manualWebProject()

        listOf("jsTest", "wasmJsTest").forEach { leaf ->
            val set = project.getKotlinExtension().sourceSets.getByName(leaf)
            val deps = set.kotlin.buildDependencies.getDependencies(null).map { it.name }
            assertTrue(
                set.kotlin.srcDirs.hasOutput(WEB_OUTPUT),
                "$leaf srcDirs: ${set.kotlin.srcDirs}",
            )
            assertTrue(WEB_TASK in deps, "$leaf deps: $deps")
        }
        assertTrue(!project.srcDirsOf("commonTest").hasOutput(WEB_OUTPUT), "commonTest must not")
    }

    // ---- B9 -----------------------------------------------------------------------------------

    @Test
    fun `GIVEN the webMain producer WHEN reading webTest THEN no plain generated fakt webTest dir is registered`() {
        val project = webProject()

        val plain =
            project.srcDirsOf("webTest").filter {
                it.path.endsWith("generated/fakt/webTest/kotlin")
            }

        assertTrue(plain.isEmpty(), "plain dirs: $plain")
        assertNotNull(project.extensions.extraProperties.get(TEST_DIR_OWNERS_PROPERTY))
    }

    @Test
    fun `GIVEN leaf wiring WHEN reading jsTest and wasmJsTest THEN no plain dir for them is registered either`() {
        val project = manualWebProject()

        listOf("jsTest", "wasmJsTest").forEach { leaf ->
            val plain =
                project.srcDirsOf(leaf).filter { it.path.endsWith("generated/fakt/$leaf/kotlin") }
            assertTrue(plain.isEmpty(), "$leaf plain dirs: $plain")
        }
    }

    // ---- B10 ----------------------------------------------------------------------------------

    @Test
    fun `GIVEN fakt disabled WHEN reading the webMain producer THEN it sees no sources at all`() {
        val project = webProject()
        project.plantMarker("webMain", "WebMarker")
        project.faktExtension().enabled.set(false)

        val task = project.webTask()

        assertTrue(task.sources.files.isEmpty(), "sources: ${task.sources.files}")
    }

    @Test
    fun `GIVEN a foreign task named faktGenerateMetadataWebMain WHEN the project is evaluated THEN the build fails naming the task`() {
        val project = createKmpProject()
        project.getKotlinExtension().apply {
            js { nodejs() }
            wasmJs { nodejs() }
        }
        project.tasks.register(WEB_TASK)

        val failure = assertFailsWith<Throwable> { project.evaluate() }

        val messages = generateSequence(failure) { it.cause }.mapNotNull { it.message }.toList()
        assertTrue(messages.any { WEB_TASK in it && "already exists" in it }, "messages: $messages")
    }

    @Test
    fun `GIVEN an evaluated project WHEN the metadata compilation is applied again THEN the test dir registry is unchanged`() {
        val project = webProject()
        val before =
            testDirOwners(project).bySourceSet.toMap() to testDirOwners(project).tasks.toSet()

        FaktGradleSubplugin()
            .applyToCompilation(
                project
                    .getKotlinExtension()
                    .targets
                    .filterIsInstance<KotlinMetadataTarget>()
                    .single()
                    .compilations
                    .getByName("webMain")
            )

        val after =
            testDirOwners(project).bySourceSet.toMap() to testDirOwners(project).tasks.toSet()
        assertEquals(before, after)
        assertEquals(
            mapOf("commonTest" to "faktGenerateMetadataCommonMain", "webTest" to WEB_TASK),
            after.first,
        )
    }

    @Test
    fun `GIVEN a metadata intermediate shared with a native target and no compiled counterpart test WHEN wiring the leaves THEN the native test set is left to its own producer`() {
        val project =
            createKmpProject().also {
                it.getKotlinExtension().apply {
                    js { nodejs() }
                    linuxX64()
                    val shared = sourceSets.create("nonJvmMain")
                    shared.dependsOn(sourceSets.getByName("commonMain"))
                    sourceSets.getByName("jsMain").dependsOn(shared)
                    sourceSets.getByName("linuxX64Main").dependsOn(shared)
                }
                it.evaluate()
            }

        val owned = testDirOwners(project).bySourceSet

        assertEquals("faktGenerateMetadataNonJvmMain", owned["jsTest"], "owned: $owned")
        assertTrue("linuxX64Test" !in owned, "native leaf must not be claimed: $owned")
    }
}
