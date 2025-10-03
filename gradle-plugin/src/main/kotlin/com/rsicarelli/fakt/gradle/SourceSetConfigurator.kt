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
            configureMultiplatformSourceSets(kotlinExtension)
        } else {
            // For single-platform projects, configure based on applied plugins
            configureJvmOnlySourceSets()
        }
    }

    /**
     * Configure multiplatform source sets to include generated fakes.
     */
    private fun configureMultiplatformSourceSets(kotlin: KotlinMultiplatformExtension) {
        kotlin.sourceSets.configureEach { sourceSet ->
            if (sourceSet.name.endsWith("Test")) {
                val generatedDir = getGeneratedSourcesDirectoryForSourceSet(sourceSet.name)
                sourceSet.kotlin.srcDir(generatedDir)

                project.logger.info(
                    "Fakt: Configured source set '${sourceSet.name}' " +
                        "to include generated sources from: $generatedDir",
                )
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
     * Since we generate fakes FROM main sources FOR test usage, we always output to test directories.
     * For KMP projects with shared code, we generate to common/test/kotlin for maximum compatibility.
     */
    fun getGeneratedSourcesDirectory(compilation: KotlinCompilation<*>): String {
        val compilationName = compilation.name
        val targetName = compilation.target.name
        val buildDir =
            project.layout.buildDirectory
                .get()
                .asFile

        // Determine if we should use common/test directory
        val shouldUseCommonTest = shouldUseCommonTestDirectory(compilationName, targetName)
        if (shouldUseCommonTest) {
            return File(buildDir, "generated/fakt/common/test/kotlin").absolutePath
        }

        // Map main compilation names to test directories
        val testDirName = mapCompilationNameToTestDir(compilationName)

        // For specific platform targets in KMP: build/generated/fakt/{targetName}/test/kotlin
        // For single-platform JVM: build/generated/fakt/test/kotlin
        return File(buildDir, "generated/fakt/$targetName/$testDirName/kotlin").absolutePath
    }

    /**
     * Determine if we should generate to common/test directory.
     */
    private fun shouldUseCommonTestDirectory(
        compilationName: String,
        targetName: String,
    ): Boolean {
        // For KMP projects, check if this is processing commonMain or metadata
        val isCommonOrMetadata = compilationName.equals("commonMain", ignoreCase = true) || targetName == "metadata"

        // Check if this is a KMP project with commonTest source set
        val hasCommonTest =
            project.extensions
                .findByType(KotlinMultiplatformExtension::class.java)
                ?.sourceSets
                ?.findByName("commonTest") != null

        return isCommonOrMetadata || hasCommonTest
    }

    /**
     * Map main compilation names to test directory names.
     * Examples: main → test, jvmMain → jvmTest, iosMain → iosTest
     */
    private fun mapCompilationNameToTestDir(compilationName: String): String =
        when {
            compilationName.equals("main", ignoreCase = true) -> "test"
            compilationName.endsWith("Main", ignoreCase = true) ->
                compilationName.removeSuffix("Main").removeSuffix("main") + "Test"

            else -> compilationName
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
