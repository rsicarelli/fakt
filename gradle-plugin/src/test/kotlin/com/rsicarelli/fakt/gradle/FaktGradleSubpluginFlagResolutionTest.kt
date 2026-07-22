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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Pins how `fakt.useExperimentalGenerateTask` is resolved and what [FaktGradleSubplugin] hands the
 * compiler for each outcome.
 *
 * Two harnesses:
 * - `ProjectBuilder` drives the public [FaktGradleSubplugin.applyToCompilation] /
 *   [FaktGradleSubplugin.isApplicable] seams directly with real compilations — covers the
 *   extension-set branch, the both-unset default, the KMP veto, and test-compilation skipping.
 * - Gradle TestKit covers the `gradleProperty` branches: `ProjectBuilder` cannot inject Gradle
 *   properties (they come from start parameters), so a synthetic build passing `-P` flags is the
 *   only faithful seam. The TestKit builds only run `tasks --all` — configuration-time probing,
 *   never task execution.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGradleSubpluginFlagResolutionTest {

    private fun Project.faktSubplugin(): FaktGradleSubplugin =
        plugins.getPlugin(FaktGradleSubplugin::class.java)

    private fun Project.faktExtension(): FaktPluginExtension =
        extensions.getByType(FaktPluginExtension::class.java)

    @Test
    fun `GIVEN flag true via extension AND JVM project WHEN applyToCompilation THEN returns single enabled false option`(
        @TempDir tempDir: File
    ) {
        val project = createJvmProject(tempDir)
        project.faktExtension().useExperimentalGenerateTask.set(true)
        project.evaluate()

        val options =
            project.faktSubplugin().applyToCompilation(project.jvmCompilation("main")).get()

        assertEquals(
            1,
            options.size,
            "Cache-correct path must hand the in-process plugin exactly one option.",
        )
        assertEquals("enabled", options.single().key)
        assertEquals(
            "false",
            options.single().value,
            "enabled=false tells the in-process registrar to exit early — generation moves to FaktGenerateTask.",
        )
    }

    @Test
    fun `GIVEN flag unset WHEN applyToCompilation on JVM THEN returns legacy options`(
        @TempDir tempDir: File
    ) {
        val project = createJvmProject(tempDir)
        project.evaluate()

        val options =
            project.faktSubplugin().applyToCompilation(project.jvmCompilation("main")).get()

        val keys = options.map { it.key }
        assertEquals(
            listOf(
                "enabled",
                "logLevel",
                "enableCallHistory",
                "enableMutableFakes",
                "sourceSetContext",
                "outputDir",
            ),
            keys,
            "Extension unset + property unset must resolve to false and keep the legacy payload.",
        )
        assertEquals("true", options.first { it.key == "enabled" }.value)
    }

    @Test
    fun `GIVEN flag true AND KMP commonMain WHEN applyToCompilation THEN registers producer and returns enabled false`() {
        val project = createKmpProject()
        project.getKotlinExtension().jvm()
        project.getKotlinExtension().linuxX64()
        project.faktExtension().useExperimentalGenerateTask.set(true)
        project.evaluate()

        val options =
            project
                .faktSubplugin()
                .applyToCompilation(project.kmpCompilation("metadata", "commonMain"))
                .get()

        assertEquals(
            "false",
            options.single { it.key == "enabled" }.value,
            "commonMain generation moves to the producer task; the in-process plugin stays off.",
        )
        assertTrue(
            project.tasks.names.any { it.startsWith("faktGenerate") },
            "The commonMain producer task must be registered for the cache-correct KMP path.",
        )
    }

    @Test
    fun `GIVEN flag true AND drivable KMP platform main WHEN applyToCompilation THEN registers consumer and returns enabled false`() {
        val project = createKmpProject()
        project.getKotlinExtension().jvm()
        project.getKotlinExtension().linuxX64()
        project.faktExtension().useExperimentalGenerateTask.set(true)
        project.evaluate()

        val options =
            project.faktSubplugin().applyToCompilation(project.kmpCompilation("jvm", "main")).get()

        assertEquals(
            1,
            options.size,
            "A drivable platform main gets enabled=false — its own source-partitioned consumer " +
                "task owns its fakes; keys: ${options.map { it.key }}",
        )
        assertEquals("enabled", options.single().key)
        assertEquals("false", options.single().value)
        assertTrue(
            project.tasks.names.contains("faktGenerateJvmMain"),
            "The drivable platform main must register a source-partitioned consumer task.",
        )
    }

    @Test
    fun `GIVEN flag true AND non-drivable KMP platform main WHEN applyToCompilation THEN returns legacy options and registers no task`() {
        val project = createKmpProject()
        project.getKotlinExtension().jvm()
        project.getKotlinExtension().linuxX64()
        project.faktExtension().useExperimentalGenerateTask.set(true)
        project.evaluate()

        val options =
            project
                .faktSubplugin()
                .applyToCompilation(project.kmpCompilation("linuxX64", "main"))
                .get()

        assertTrue(
            options.any { it.key == "sourceSetContext" },
            "A non-drivable platform main keeps the in-process plugin (legacy hybrid) so it still " +
                "generates its platform-specific fakes; keys: ${options.map { it.key }}",
        )
        assertTrue(
            project.tasks.names.none { it == "faktGenerateLinuxX64Main" },
            "Native mains can't be driven in-process — no FaktGenerateTask for them.",
        )
    }

    @Test
    fun `GIVEN flag true AND KMP without a drivable target WHEN applyToCompilation on commonMain THEN registers the metadata producer`() {
        val project = createKmpProject()
        project.getKotlinExtension().linuxX64()
        project.getKotlinExtension().mingwX64()
        project.faktExtension().useExperimentalGenerateTask.set(true)
        project.evaluate()

        val options =
            project
                .faktSubplugin()
                .applyToCompilation(project.kmpCompilation("metadata", "commonMain"))
                .get()

        assertEquals(
            "false",
            options.single { it.key == "enabled" }.value,
            "KotlinMetadataCompiler needs no JVM classpath — commonMain is a producer even " +
                "without a JVM/Android target; keys: ${options.map { it.key }}",
        )
        assertTrue(
            project.tasks.names.contains("faktGenerateMetadataCommonMain"),
            "No-JVM-target KMP must still register the cache-correct common producer task; " +
                "tasks: ${project.tasks.names.filter { it.startsWith("fakt") }}",
        )
    }

    @Test
    fun `GIVEN flag true AND KMP without a drivable target WHEN applyToCompilation on native main THEN stays legacy hybrid`() {
        val project = createKmpProject()
        project.getKotlinExtension().linuxX64()
        project.getKotlinExtension().mingwX64()
        project.faktExtension().useExperimentalGenerateTask.set(true)
        project.evaluate()

        val options =
            project
                .faktSubplugin()
                .applyToCompilation(project.kmpCompilation("linuxX64", "main"))
                .get()

        assertTrue(
            options.any { it.key == "sourceSetContext" },
            "A native main keeps the in-process plugin (legacy hybrid) so its platform fakes are " +
                "still generated; keys: ${options.map { it.key }}",
        )
        assertTrue(
            project.tasks.names.none { it == "faktGenerateLinuxX64Main" },
            "Native mains can't be driven by the worker — no FaktGenerateTask for them.",
        )
    }

    @Test
    fun `GIVEN JVM test compilation WHEN isApplicable THEN returns false`(@TempDir tempDir: File) {
        val project = createJvmProject(tempDir)
        project.evaluate()
        val subplugin = project.faktSubplugin()

        assertFalse(
            subplugin.isApplicable(project.jvmCompilation("test")),
            "Test compilations only consume generated fakes — no FaktGenerateTask for them.",
        )
        assertTrue(
            subplugin.isApplicable(project.jvmCompilation("main")),
            "Main compilations carry the @Fake declarations and must stay applicable.",
        )
    }

    @Test
    fun `GIVEN testFixtures compilation WHEN isApplicable THEN returns false`(
        @TempDir tempDir: File
    ) {
        val project = createJvmProject(tempDir)
        project.pluginManager.apply("java-test-fixtures")
        project.evaluate()
        val subplugin = project.faktSubplugin()

        // The testFixtures compilation is NOT flagged `isTestCompilation`, so without the explicit
        // name exclusion `isApplicable` would return true and the plugin would run generation over
        // the fixtures sources (self-referential wiring). AGP's
        // debugTestFixtures/releaseTestFixtures
        // behave the same; the JVM `testFixtures` compilation is the SDK-free proxy for that case.
        assertFalse(
            subplugin.isApplicable(project.jvmCompilation("testFixtures")),
            "The testFixtures compilation consumes generated fakes — it must be excluded from " +
                "generation even though it is not a test compilation.",
        )
    }

    // -- TestKit: gradleProperty branches ---------------------------------------------------------

    @Test
    fun `GIVEN gradle property true and extension unset WHEN building THEN faktGenerateJvmMain registered`(
        @TempDir projectDir: File
    ) {
        setupKotlinJvmFaktProject(projectDir)

        val result = listTasks(projectDir, "-Pfakt.useExperimentalGenerateTask=true")

        assertTrue(
            result.output.contains("faktGenerateJvmMain"),
            "Property true with extension unset must win and register the generate task.",
        )
    }

    @Test
    fun `GIVEN gradle property yes WHEN building THEN treated as false and no faktGenerate task`(
        @TempDir projectDir: File
    ) {
        setupKotlinJvmFaktProject(projectDir)

        val result = listTasks(projectDir, "-Pfakt.useExperimentalGenerateTask=yes")

        assertFalse(
            result.output.contains("faktGenerate"),
            "Only strict 'true'/'false' parse — 'yes' must fall back to false.",
        )
    }

    @Test
    fun `GIVEN gradle property empty string WHEN building THEN treated as false and no faktGenerate task`(
        @TempDir projectDir: File
    ) {
        setupKotlinJvmFaktProject(projectDir)

        val result = listTasks(projectDir, "-Pfakt.useExperimentalGenerateTask=")

        assertFalse(
            result.output.contains("faktGenerate"),
            "Empty property value must fall back to false, not crash or enable the path.",
        )
    }

    @Test
    fun `GIVEN extension false and gradle property true WHEN building THEN property wins and task registered`(
        @TempDir projectDir: File
    ) {
        setupKotlinJvmFaktProject(
            projectDir,
            extensionBlock =
                "extensions.getByType(com.rsicarelli.fakt.gradle.FaktPluginExtension::class.java)" +
                    ".useExperimentalGenerateTask.set(false)",
        )

        val result = listTasks(projectDir, "-Pfakt.useExperimentalGenerateTask=true")

        assertTrue(
            result.output.contains("faktGenerateJvmMain"),
            "Extension false does not veto the property — only extension TRUE short-circuits.",
        )
    }

    @Test
    fun `GIVEN extension true and gradle property false WHEN building THEN property wins and no faktGenerate task`(
        @TempDir projectDir: File
    ) {
        setupKotlinJvmFaktProject(
            projectDir,
            extensionBlock =
                "extensions.getByType(com.rsicarelli.fakt.gradle.FaktPluginExtension::class.java)" +
                    ".useExperimentalGenerateTask.set(true)",
        )

        val result = listTasks(projectDir, "-Pfakt.useExperimentalGenerateTask=false")

        assertFalse(
            result.output.contains("faktGenerate"),
            "An explicit property false must override extension true so users can always opt out.",
        )
    }

    /**
     * Writes a synthetic Kotlin JVM project applying the real KGP + Fakt plugins from this test
     * JVM's classpath. Unlike [FaktGenerateTaskTest]'s worker classpath, the buildscript classpath
     * keeps `kotlin-gradle-plugin` — it is exactly what the synthetic build applies.
     */
    private fun setupKotlinJvmFaktProject(projectDir: File, extensionBlock: String = "") {
        val classpathLiteral =
            System.getProperty("java.class.path")
                .split(File.pathSeparator)
                .map(::File)
                .filter { it.exists() && "kctfork" !in it.absolutePath }
                .joinToString(",\n        ") { entry ->
                    """file("${entry.absolutePath.replace('\\', '/')}")"""
                }
        projectDir
            .resolve("settings.gradle.kts")
            .writeText("""rootProject.name = "fakt-flag-test"""")
        projectDir
            .resolve("gradle.properties")
            .writeText("org.gradle.jvmargs=-Xmx2g -XX:MaxMetaspaceSize=1024m\n")
        projectDir
            .resolve("build.gradle.kts")
            .writeText(
                """
                buildscript {
                    dependencies {
                        classpath(files($classpathLiteral))
                    }
                }

                apply(plugin = "org.jetbrains.kotlin.jvm")
                apply(plugin = "com.rsicarelli.fakt")

                $extensionBlock
                """
                    .trimIndent()
            )
    }

    private fun listTasks(projectDir: File, vararg arguments: String): BuildResult =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .forwardOutput()
            .withArguments("tasks", "--all", *arguments, "--stacktrace")
            .build()
}
