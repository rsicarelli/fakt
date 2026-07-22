// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Pins the JVM-only and testFixtures branches of [SourceSetConfigurator].
 *
 * The KMP branch (`configureKmpSourceSets`) is covered by [SimplifiedSourceSetConfigurationTest];
 * this suite covers the `configureJvmSourceSets` task wiring, the `isTestTask` classification, and
 * the `configureTestFixturesSourceSet` path with and without the `java-test-fixtures` plugin.
 *
 * The compile task's `sources` property resolves as a live file tree, so each test plants a marker
 * `.kt` file inside the expected generated directory and asserts on its presence (or absence) in
 * the realized compile task's sources — no task execution needed.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SourceSetConfiguratorJvmTest {

    private fun createKotlinJvmProject(projectDir: File): Project {
        val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
        project.group = "com.example"
        project.version = "1.0.0"
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        return project
    }

    /** Plants a marker file so the file tree behind the task's `sources` can see the dir. */
    private fun plantMarker(project: Project, relativeDir: String): File {
        val dir = File(project.layout.buildDirectory.get().asFile, relativeDir)
        dir.mkdirs()
        return File(dir, "Marker.kt").apply { writeText("// marker\n") }
    }

    private fun kotlinCompileTask(project: Project, name: String): AbstractKotlinCompile<*> =
        project.tasks.getByName(name) as AbstractKotlinCompile<*>

    @Test
    fun `GIVEN JVM project without test fixtures WHEN configureSourceSets THEN compileTestKotlin includes generated test dir`(
        @TempDir tempDir: File
    ) {
        val project = createKotlinJvmProject(tempDir)
        val marker = plantMarker(project, "generated/fakt/test/kotlin")

        SourceSetConfigurator(project, useTestFixtures = false).configureSourceSets()

        val testTask = kotlinCompileTask(project, "compileTestKotlin")
        assertTrue(
            testTask.sources.files.contains(marker),
            "compileTestKotlin must compile build/generated/fakt/test/kotlin; " +
                "sources: ${testTask.sources.files}",
        )
    }

    @Test
    fun `GIVEN JVM project WHEN configureSourceSets THEN compileKotlin main does NOT include generated dir`(
        @TempDir tempDir: File
    ) {
        val project = createKotlinJvmProject(tempDir)
        val marker = plantMarker(project, "generated/fakt/test/kotlin")

        SourceSetConfigurator(project, useTestFixtures = false).configureSourceSets()

        val mainTask = kotlinCompileTask(project, "compileKotlin")
        assertFalse(
            mainTask.sources.files.contains(marker),
            "Generated fakes must never leak into the main compilation.",
        )
    }

    @Test
    fun `GIVEN useTestFixtures true AND java-test-fixtures applied WHEN configureSourceSets THEN testFixtures java srcDir includes generated dir`(
        @TempDir tempDir: File
    ) {
        val project = createKotlinJvmProject(tempDir)
        project.pluginManager.apply("java-test-fixtures")
        val generatedDir =
            File(project.layout.buildDirectory.get().asFile, "generated/fakt/testFixtures/kotlin")

        SourceSetConfigurator(project, useTestFixtures = true).configureSourceSets()

        val testFixturesSourceSet =
            project.extensions
                .getByType(JavaPluginExtension::class.java)
                .sourceSets
                .getByName("testFixtures")
        assertTrue(
            testFixturesSourceSet.java.srcDirs.contains(generatedDir),
            "testFixtures java source set must include the generated dir; " +
                "srcDirs: ${testFixturesSourceSet.java.srcDirs}",
        )
    }

    @Test
    fun `GIVEN useTestFixtures true AND java-test-fixtures applied WHEN configureSourceSets THEN compileTestFixturesKotlin sources include generated dir`(
        @TempDir tempDir: File
    ) {
        val project = createKotlinJvmProject(tempDir)
        project.pluginManager.apply("java-test-fixtures")
        val marker = plantMarker(project, "generated/fakt/testFixtures/kotlin")

        SourceSetConfigurator(project, useTestFixtures = true).configureSourceSets()

        // The Kotlin-compile-task route (Route 2) is the ONLY one that reaches AGP's
        // compile*TestFixturesKotlin tasks, which have no JavaPluginExtension backing. Proving it
        // on
        // the JVM `compileTestFixturesKotlin` task exercises the same mechanism without the Android
        // SDK (ProjectBuilder cannot apply com.android.library).
        val fixturesTask = kotlinCompileTask(project, "compileTestFixturesKotlin")
        assertTrue(
            fixturesTask.sources.files.contains(marker),
            "compileTestFixturesKotlin must source build/generated/fakt/testFixtures/kotlin; " +
                "sources: ${fixturesTask.sources.files}",
        )
    }

    @Test
    fun `GIVEN useTestFixtures true AND java-test-fixtures NOT applied WHEN configureSourceSets THEN completes without error`(
        @TempDir tempDir: File
    ) {
        val project = createKotlinJvmProject(tempDir)

        SourceSetConfigurator(project, useTestFixtures = true).configureSourceSets()

        assertNull(
            project.extensions
                .getByType(JavaPluginExtension::class.java)
                .sourceSets
                .findByName("testFixtures"),
            "Without java-test-fixtures the source set must not exist — wiring is a no-op.",
        )
    }

    @Test
    fun `GIVEN useTestFixtures true WHEN configureSourceSets THEN JVM test task wiring is skipped`(
        @TempDir tempDir: File
    ) {
        val project = createKotlinJvmProject(tempDir)
        project.pluginManager.apply("java-test-fixtures")
        val marker = plantMarker(project, "generated/fakt/test/kotlin")

        SourceSetConfigurator(project, useTestFixtures = true).configureSourceSets()

        val testTask = kotlinCompileTask(project, "compileTestKotlin")
        assertFalse(
            testTask.sources.files.contains(marker),
            "testFixtures mode must short-circuit the default test source-set wiring.",
        )
    }
}
