// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import java.io.File

/**
 * Configures source sets and output directories for generated fake implementations.
 *
 * This internal utility handles the complex task of determining where to place generated
 * fakes and ensuring they're accessible from test compilations. It supports both
 * single-platform (JVM-only) and Kotlin Multiplatform (KMP) projects.
 *
 * ## Directory Strategy
 *
 * Generated fakes are ALWAYS placed in test source sets, never in main/production:
 * ```
 * build/generated/fakt/
 * ├── commonTest/kotlin/     # KMP: Common test fakes
 * ├── jvmTest/kotlin/        # KMP: JVM-specific test fakes
 * ├── test/kotlin/           # Single-platform: Test fakes
 * └── fakes/kotlin/          # KMP: Shared fakes (metadata target)
 * ```
 *
 * ## KMP Source Set Mapping
 *
 * For multiplatform projects, generated fakes are added to existing test source sets:
 * - `commonTest` → `build/generated/fakt/commonTest/kotlin`
 * - `jvmTest` → `build/generated/fakt/jvmTest/kotlin`
 * - `iosTest` → `build/generated/fakt/iosTest/kotlin`
 *
 * ## Usage
 *
 * This class is used internally by [FaktGradleSubplugin] during plugin application:
 * ```kotlin
 * val configurator = SourceSetConfigurator(project)
 * configurator.configureSourceSets()  // Auto-configures based on project type
 * val outputDir = configurator.getGeneratedSourcesDirectory(compilation)
 * ```
 *
 * @property project The Gradle project to configure
 * @see FaktGradleSubplugin
 * @see SourceSetDiscovery
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
                val generatedDir = File(buildDir, "generated/fakt/${sourceSet.name}/kotlin")
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
     * Since we generate fakes FROM main sources FOR test source sets, we output to test directories.
     * For KMP projects with shared code, we generate to fakes/kotlin for maximum compatibility.
     *
     * ## Directory Resolution Examples
     *
     * **KMP with commonMain:**
     * ```
     * compilation.target.name = "metadata"
     * → returns: "build/generated/fakt/fakes/kotlin"
     * ```
     *
     * **KMP with jvmMain:**
     * ```
     * compilation.target.name = "jvm"
     * → returns: "build/generated/fakt/jvmTest/kotlin"
     * ```
     *
     * **Single-platform JVM:**
     * ```
     * compilation.target.name = "jvm"
     * → returns: "build/generated/fakt/test/kotlin"
     * ```
     *
     * @param compilation The Kotlin compilation to get the output directory for
     * @return Absolute path to the generated sources directory for this compilation
     * @see shouldUseCommonFakesDirectory
     */
    fun getGeneratedSourcesDirectory(compilation: KotlinCompilation<*>): String {
        val targetName = compilation.target.name
        val buildDir =
            project.layout.buildDirectory
                .get()
                .asFile

        // Determine if we should use common fakes directory
        val shouldUseCommonFakes = shouldUseCommonFakesDirectory(targetName)
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
     *
     * ## Decision Logic
     *
     * ```
     * targetName == "metadata" → true (always use common directory)
     * hasCommonTest() → true (shared fakes needed)
     * otherwise → false (use platform-specific directory)
     * ```
     *
     * @param targetName The Kotlin compilation target name (e.g., "metadata", "jvm", "js")
     * @return `true` if fakes should be generated to common directory, `false` for platform-specific
     */
    private fun shouldUseCommonFakesDirectory(targetName: String): Boolean {
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
}
