// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.createJvmProject
import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import com.rsicarelli.fakt.gradle.helpers.jvmCompilation
import com.rsicarelli.fakt.gradle.helpers.kmpCompilation
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.Task
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Pins the per-compilation contract of [FaktGenerateTaskWiring.register]: task naming, idempotent
 * registration, configuration bootstrap, test-source-set wiring (JVM and KMP variants), and the KMP
 * cross-target `dependsOn` chain.
 *
 * Each test drives `register` directly with a **real** [KotlinCompilation] obtained from
 * `ProjectBuilder` + the Kotlin Gradle Plugin — the minimal fakes in `fakes/` throw on members
 * `register` dereferences (`allKotlinSourceSets`, `compileDependencyFiles`). KGP does not invoke
 * `applyToCompilation` under `ProjectBuilder`, so calling `register` directly is the unit seam; the
 * flag-driven end-to-end path is covered by [FaktGradleSubpluginFlagResolutionTest] and the CI
 * sample matrix.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateTaskWiringTest {

    private fun evaluatedJvmProject(projectDir: File): Project =
        createJvmProject(projectDir).also { it.evaluate() }

    /** KMP project with jvm + linuxX64 targets so the metadata `commonMain` compilation exists. */
    private fun evaluatedKmpProject(): Project =
        createKmpProject().also { project ->
            project.getKotlinExtension().jvm()
            project.getKotlinExtension().linuxX64()
            project.evaluate()
        }

    private fun Project.faktExtension(): FaktPluginExtension =
        extensions.getByType(FaktPluginExtension::class.java)

    private fun Project.registerWiring(targetName: String, compilationName: String) {
        FaktGenerateTaskWiring.register(
            this,
            kmpCompilation(targetName, compilationName),
            faktExtension(),
        )
    }

    @Test
    fun `GIVEN JVM main compilation WHEN register THEN registers task named faktGenerateJvmMain`(
        @TempDir tempDir: File
    ) {
        val project = evaluatedJvmProject(tempDir)

        FaktGenerateTaskWiring.register(
            project,
            project.jvmCompilation("main"),
            project.faktExtension(),
        )

        assertNotNull(
            project.tasks.findByName("faktGenerateJvmMain"),
            "Single-platform JVM has a blank targetName — the platform-type fallback must " +
                "produce faktGenerateJvmMain; tasks: ${project.tasks.names.filter { it.startsWith("fakt") }}",
        )
    }

    @Test
    fun `GIVEN KMP metadata commonMain compilation WHEN register THEN registers task named faktGenerateMetadataCommonMain`() {
        val project = evaluatedKmpProject()

        project.registerWiring("metadata", "commonMain")

        assertNotNull(
            project.tasks.findByName("faktGenerateMetadataCommonMain"),
            "Metadata commonMain compilation must map to faktGenerateMetadataCommonMain; " +
                "tasks: ${project.tasks.names.filter { it.startsWith("fakt") }}",
        )
    }

    @Test
    fun `GIVEN register called twice for same compilation WHEN second call THEN no duplicate task and no error`(
        @TempDir tempDir: File
    ) {
        val project = evaluatedJvmProject(tempDir)
        val compilation = project.jvmCompilation("main")
        val extension = project.faktExtension()
        FaktGenerateTaskWiring.register(project, compilation, extension)

        // Must be a silent no-op: without the existence guard Gradle throws on duplicate names.
        FaktGenerateTaskWiring.register(project, compilation, extension)

        assertNotNull(project.tasks.findByName("faktGenerateJvmMain"))
    }

    @Test
    fun `GIVEN JVM project WHEN register THEN faktWorker and faktCompiler configurations created`(
        @TempDir tempDir: File
    ) {
        val project = evaluatedJvmProject(tempDir)

        FaktGenerateTaskWiring.register(
            project,
            project.jvmCompilation("main"),
            project.faktExtension(),
        )

        assertNotNull(
            project.configurations.findByName(FaktGradleSubplugin.WORKER_CONFIGURATION),
            "register fires before afterEvaluate in the real flow — it must bootstrap faktWorker.",
        )
        assertNotNull(
            project.configurations.findByName(FaktGradleSubplugin.COMPILER_CLASSPATH_CONFIGURATION),
            "register must bootstrap faktCompiler alongside faktWorker.",
        )
    }

    @Test
    fun `GIVEN flag created configurations on evaluate WHEN register runs afterwards THEN configuration creation is idempotent`(
        @TempDir tempDir: File
    ) {
        val project = createJvmProject(tempDir)
        project.faktExtension().useExperimentalGenerateTask.set(true)
        project.evaluate()
        assertNotNull(
            project.configurations.findByName(FaktGradleSubplugin.WORKER_CONFIGURATION),
            "Precondition: afterEvaluate with flag=true must have created faktWorker.",
        )

        // Second creation site (registration-time) must be safe after the afterEvaluate one.
        FaktGenerateTaskWiring.register(
            project,
            project.jvmCompilation("main"),
            project.faktExtension(),
        )

        assertNotNull(project.tasks.findByName("faktGenerateJvmMain"))
        assertNotNull(project.configurations.findByName(FaktGradleSubplugin.WORKER_CONFIGURATION))
        assertNotNull(
            project.configurations.findByName(FaktGradleSubplugin.COMPILER_CLASSPATH_CONFIGURATION)
        )
    }

    @Test
    fun `GIVEN JVM main compilation WHEN register THEN generated dir wired into compileTestKotlin sources`(
        @TempDir tempDir: File
    ) {
        val project = evaluatedJvmProject(tempDir)
        // The compile task's `sources` resolves as a live file tree — plant a marker so the
        // wired directory is observable without executing the generate task.
        val generatedDir =
            File(project.layout.buildDirectory.get().asFile, "generated/fakt/jvm/main/kotlin")
        generatedDir.mkdirs()
        val marker = File(generatedDir, "Marker.kt").apply { writeText("// marker\n") }

        FaktGenerateTaskWiring.register(
            project,
            project.jvmCompilation("main"),
            project.faktExtension(),
        )

        val testTask = project.tasks.getByName("compileTestKotlin") as AbstractKotlinCompile<*>
        assertTrue(
            testTask.sources.files.contains(marker),
            "JVM-only projects wire the generated dir into *Test compile tasks via source(); " +
                "sources: ${testTask.sources.files}",
        )
    }

    @Test
    fun `GIVEN KMP jvm main compilation WHEN register THEN generated dir added to jvmTest kotlin srcDir`() {
        val project = evaluatedKmpProject()

        project.registerWiring("jvm", "main")

        val jvmTestSrcDirs =
            project.getKotlinExtension().sourceSets.getByName("jvmTest").kotlin.srcDirs
        assertTrue(
            jvmTestSrcDirs.any { it.path.endsWith("generated/fakt/jvm/main/kotlin") },
            "KMP projects wire the generated dir as a jvmTest srcDir via the task provider; " +
                "srcDirs: $jvmTestSrcDirs",
        )
    }

    @Test
    fun `GIVEN KMP jvm main compilation WHEN register after commonMain THEN platform task dependsOn metadata counterpart`() {
        val project = evaluatedKmpProject()
        project.registerWiring("metadata", "commonMain")
        val commonTask = project.tasks.getByName("faktGenerateMetadataCommonMain")

        project.registerWiring("jvm", "main")

        // taskProvider.configure {} is lazy — realizing the task applies the dependsOn wiring.
        val platformTask = project.tasks.getByName("faktGenerateJvmMain")
        assertTrue(
            platformTask.dependsOn.contains(commonTask),
            "Platform faktGenerate tasks must depend on the commonMain counterpart so common " +
                "@Fake declarations are validated once; dependsOn: ${platformTask.dependsOn}",
        )
    }

    @Test
    fun `GIVEN KMP metadata commonMain compilation WHEN register THEN no dependsOn wiring added`() {
        val project = evaluatedKmpProject()

        project.registerWiring("metadata", "commonMain")

        val commonTask = project.tasks.getByName("faktGenerateMetadataCommonMain")
        val faktTaskDependencies =
            commonTask.dependsOn.filterIsInstance<Task>().filter {
                it.name.startsWith("faktGenerate")
            }
        assertTrue(
            faktTaskDependencies.isEmpty(),
            "commonMain is the root of the chain — it must not depend on any faktGenerate task; " +
                "found: $faktTaskDependencies",
        )
    }
}
