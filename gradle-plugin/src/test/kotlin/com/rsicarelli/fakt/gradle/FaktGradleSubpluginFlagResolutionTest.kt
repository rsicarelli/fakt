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
    fun `GIVEN flag true via extension AND KMP project WHEN applyToCompilation THEN returns legacy options`() {
        val project = createKmpProject()
        project.getKotlinExtension().jvm()
        project.faktExtension().useExperimentalGenerateTask.set(true)
        project.evaluate()

        val options =
            project.faktSubplugin().applyToCompilation(project.kmpCompilation("jvm", "main")).get()

        assertTrue(
            options.any { it.key == "sourceSetContext" },
            "KMP must veto the cache-correct path — the worker only drives K2JVMCompiler; " +
                "keys: ${options.map { it.key }}",
        )
        assertTrue(project.tasks.names.none { it.startsWith("faktGenerate") })
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
