// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
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
    fun configureSourceSets() =
        project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            ?.let(::configureKmpSourceSets)
            ?: configureJvmSourceSets()

    /**
     * Configure multiplatform source sets to include generated fakes.
     *
     * This adds generated directories to EXISTING test source sets.
     * Generated fakes are placed in directories matching the test source set names:
     * - commonTest → build/generated/fakt/commonTest/kotlin
     * - jvmTest → build/generated/fakt/jvmTest/kotlin
     * - etc.
     */
    private fun configureKmpSourceSets(kotlin: KotlinMultiplatformExtension) {
        val buildDir = project.layout.buildDirectory.get().asFile

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
    private fun configureJvmSourceSets() {
        // For JVM-only projects, add generated sources to test source sets
        project.tasks.withType(KotlinCompile::class.java) { task ->
            if (task.name.contains("Test", ignoreCase = true)) {
                val generatedDir = File(
                    project.layout.buildDirectory.get().asFile,
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
}
