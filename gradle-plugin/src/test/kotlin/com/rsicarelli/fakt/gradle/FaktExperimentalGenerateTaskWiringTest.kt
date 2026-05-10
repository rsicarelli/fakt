// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * BDD coverage for the experimental `fakt.useExperimentalGenerateTask` rollout switch (PR 3).
 *
 * End-to-end behaviour (per-compilation `FaktGenerateTask` registration,
 * `kotlin.srcDir(taskProvider)` wiring, KMP `dependsOn` ordering) requires the Kotlin Gradle Plugin
 * to be applied — not feasible inside `ProjectBuilder` because the KGP wiring is heavy. Those flows
 * are covered by the sample matrix in CI (`samples/jvm-single-module:build
 * -Pfakt.useExperimentalGenerateTask=true`); this suite focuses on the parts that are
 * unit-testable: extension defaults, property opt-in, and the resolvable configurations the worker
 * classpath rides on.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktExperimentalGenerateTaskWiringTest {

    @Test
    fun `GIVEN plugin applied WHEN reading useExperimentalGenerateTask THEN defaults to false`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.rsicarelli.fakt")

        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        assertFalse(
            extension.useExperimentalGenerateTask.get(),
            "Default must be false so the legacy in-process path stays the rollout target.",
        )
    }

    @Test
    fun `GIVEN extension WHEN setting useExperimentalGenerateTask THEN flag flips`() {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.rsicarelli.fakt")
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        extension.useExperimentalGenerateTask.set(true)

        assertTrue(extension.useExperimentalGenerateTask.get())
    }

    @Test
    fun `GIVEN flag false on apply WHEN evaluating THEN faktWorker faktCompiler configurations are NOT created`(
        @TempDir tempDir: java.io.File
    ) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("com.rsicarelli.fakt")

        (project as ProjectInternal).evaluate()

        assertNull(
            project.configurations.findByName(FaktGradleSubplugin.WORKER_CONFIGURATION),
            "faktWorker should only be created when the flag is on — legacy path stays clean.",
        )
        assertNull(
            project.configurations.findByName(FaktGradleSubplugin.COMPILER_CLASSPATH_CONFIGURATION),
            "faktCompiler should only be created when the flag is on.",
        )
    }

    @Test
    fun `GIVEN flag set true via extension WHEN evaluating THEN faktWorker and faktCompiler configurations exist`(
        @TempDir tempDir: java.io.File
    ) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("com.rsicarelli.fakt")
        project.extensions
            .getByType(FaktPluginExtension::class.java)
            .useExperimentalGenerateTask
            .set(true)

        (project as ProjectInternal).evaluate()

        val worker = project.configurations.findByName(FaktGradleSubplugin.WORKER_CONFIGURATION)
        val compiler =
            project.configurations.findByName(FaktGradleSubplugin.COMPILER_CLASSPATH_CONFIGURATION)
        assertNotNull(worker, "faktWorker configuration must be created when flag=true.")
        assertNotNull(compiler, "faktCompiler configuration must be created when flag=true.")
        assertTrue(worker.isCanBeResolved, "faktWorker must be resolvable for worker classpath.")
        assertFalse(
            worker.isCanBeConsumed,
            "faktWorker is internal — never participates in inter-project resolution.",
        )
    }

    @Test
    fun `GIVEN flag set true via extension WHEN evaluating THEN faktWorker has kotlin-compiler-embeddable + faktCompiler has Fakt compiler artifact`(
        @TempDir tempDir: java.io.File
    ) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("com.rsicarelli.fakt")
        project.extensions
            .getByType(FaktPluginExtension::class.java)
            .useExperimentalGenerateTask
            .set(true)

        (project as ProjectInternal).evaluate()

        val workerDeps =
            project.configurations.getByName(FaktGradleSubplugin.WORKER_CONFIGURATION).dependencies
        assertTrue(
            workerDeps.any {
                it.group == "org.jetbrains.kotlin" && it.name == "kotlin-compiler-embeddable"
            },
            "faktWorker must include kotlin-compiler-embeddable; current deps: ${workerDeps.map { "${it.group}:${it.name}" }}",
        )
        val compilerDeps =
            project.configurations
                .getByName(FaktGradleSubplugin.COMPILER_CLASSPATH_CONFIGURATION)
                .dependencies
        assertTrue(
            compilerDeps.any {
                it.group == FaktGradleSubplugin.PLUGIN_GROUP_ID &&
                    it.name == FaktGradleSubplugin.PLUGIN_ARTIFACT_NAME
            },
            "faktCompiler must include Fakt's :compiler artifact; current deps: ${compilerDeps.map { "${it.group}:${it.name}" }}",
        )
    }
}
