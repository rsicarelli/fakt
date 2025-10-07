// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import java.io.File

/**
 * Helper class to configure source sets and output directories for generated fakes.
 */
internal class SourceSetConfigurator(
    private val project: Project,
) {
    /**
     * Automatically configure source sets for multiplatform projects.
     * This ensures generated fakes are accessible from test source sets.
     */
    fun configureSourceSets() {
        // Check if this is a multiplatform project
        val kotlinExtension =
            project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        if (kotlinExtension != null) {
            // Configure generated sources directories in EXISTING test source sets
            configureMultiplatformSourceSets(kotlinExtension)
        } else {
            // For single-platform projects, configure based on applied plugins
            configureJvmOnlySourceSets()
        }
    }

    /**
     * Configure multiplatform source sets to include generated fakes.
     *
     * This adds generated directories to EXISTING test source sets.
     * Generated fakes are placed in directories matching the test source set names:
     * - commonTest → build/generated/fakt/commonTest/kotlin
     * - jvmTest → build/generated/fakt/jvmTest/kotlin
     * - etc.
     */
    private fun configureMultiplatformSourceSets(kotlin: KotlinMultiplatformExtension) {
        val buildDir =
            project.layout.buildDirectory
                .get()
                .asFile

        kotlin.sourceSets.configureEach { sourceSet ->
            // Add generated directory to ALL test source sets using consistent naming
            if (sourceSet.name.endsWith("Test")) {
                val generatedDir = java.io.File(buildDir, "generated/fakt/${sourceSet.name}/kotlin")
                sourceSet.kotlin.srcDir(generatedDir)
                project.logger.info("Fakt: Added generated dir to ${sourceSet.name}: $generatedDir")
            }
        }
    }

    /**
     * Configure JVM-only projects.
     */
    private fun configureJvmOnlySourceSets() {
        // For JVM-only projects, add generated sources to test source sets
        project.tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) { task ->
            if (task.name.contains("Test", ignoreCase = true)) {
                val generatedDir =
                    File(
                        project.layout.buildDirectory
                            .get()
                            .asFile,
                        "generated/fakt/test/kotlin",
                    )
                task.source(generatedDir)

                project.logger.info(
                    "Fakt: Configured test compilation task '${task.name}' " +
                        "to include generated sources",
                )
            }
        }
    }

    /**
     * Get the appropriate generated sources directory for a compilation.
     *
     * Since we generate fakes FROM main sources FOR fakes source sets, we output to fakes directories.
     * For KMP projects with shared code, we generate to fakes/kotlin for maximum compatibility.
     */
    fun getGeneratedSourcesDirectory(compilation: KotlinCompilation<*>): String {
        val compilationName = compilation.name
        val targetName = compilation.target.name
        val buildDir =
            project.layout.buildDirectory
                .get()
                .asFile

        // Determine if we should use common fakes directory
        val shouldUseCommonFakes = shouldUseCommonFakesDirectory(compilationName, targetName)
        if (shouldUseCommonFakes) {
            return File(buildDir, "generated/fakt/fakes/kotlin").absolutePath
        }

        // Map target names to fakes directories
        // For JVM target: jvm → jvmFakes
        // For JS target: js → jsFakes
        val fakesDirName = "${targetName}Fakes"

        // For specific platform targets in KMP: build/generated/fakt/{targetName}Fakes/kotlin
        return File(buildDir, "generated/fakt/$fakesDirName/kotlin").absolutePath
    }

    /**
     * Determine if we should generate to common fakes directory.
     *
     * We generate to 'fakes/kotlin' (common directory) when:
     * 1. Target is 'metadata' (represents commonMain compilation)
     * 2. Project has commonTest source set (tests need shared fakes)
     *
     * This ensures fakes are accessible from commonTest source set.
     */
    private fun shouldUseCommonFakesDirectory(
        compilationName: String,
        targetName: String,
    ): Boolean {
        // Check if metadata target (commonMain compilation)
        if (targetName == "metadata") {
            return true
        }

        // Check if project has commonTest source set
        // If yes, generate to common directory so commonTest can access fakes
        val kotlinExtension =
            project.extensions.findByType(KotlinMultiplatformExtension::class.java)

        return kotlinExtension?.sourceSets?.any { it.name == "commonTest" } ?: false
    }

    /**
     * Get generated sources directory for a specific source set.
     */
    private fun getGeneratedSourcesDirectoryForSourceSet(sourceSetName: String): File {
        val buildDir =
            project.layout.buildDirectory
                .get()
                .asFile

        return when {
            sourceSetName == "commonTest" ->
                File(buildDir, "generated/fakt/common/test/kotlin")

            sourceSetName.endsWith("Test") -> {
                val target = sourceSetName.removeSuffix("Test")
                File(buildDir, "generated/fakt/$target/test/kotlin")
            }

            else ->
                File(buildDir, "generated/fakt/common/test/kotlin")
        }
    }
}
