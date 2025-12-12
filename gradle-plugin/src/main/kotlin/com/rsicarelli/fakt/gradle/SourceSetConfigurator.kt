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
 * - `jvmTest` → `build/generated/fakt/jvmTest/kotlin` + `commonTest/kotlin`
 * - `iosTest` → `build/generated/fakt/iosTest/kotlin` + `commonTest/kotlin`
 *
 * Platform test source sets (jvmTest, iosTest, etc.) are configured to see BOTH:
 * 1. Their own generated directory (for platform-specific fakes)
 * 2. The commonTest generated directory (for common interfaces from commonMain)
 *
 * This dual registration ensures platform tests can import fakes generated from
 * common interfaces while maintaining support for platform-specific fakes.
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
        project.extensions
            .findByType(KotlinMultiplatformExtension::class.java)
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
        val buildDir =
            project.layout.buildDirectory
                .get()
                .asFile

        // PASS 1: Add source-set-specific generated directories
        // Each test source set gets its own generated directory
        // Example: jvmTest → build/generated/fakt/jvmTest/kotlin
        kotlin.sourceSets.configureEach { sourceSet ->
            if (sourceSet.name.endsWith("Test")) {
                val generatedDir = File(buildDir, "generated/fakt/${sourceSet.name}/kotlin")
                sourceSet.kotlin.srcDir(generatedDir)
                project.logger.info("Fakt: Added generated dir to ${sourceSet.name}: $generatedDir")
            }
        }

        // PASS 2: Add commonTest directory to LEAF platform test source sets only
        // This is CRITICAL for KMP projects where the compiler outputs fakes to commonTest
        // when the interface is in commonMain (see SourceSetDiscovery.kt:176-184)
        //
        // Why: Compiler outputs to commonTest for common interfaces, but platform tests
        // (iosX64Test, jvmTest, etc.) need to see those fakes too.
        //
        // IMPORTANT: We skip intermediate source sets (nativeTest, appleTest, etc.) because
        // they already have a dependency relationship with commonTest through KMP's hierarchy.
        // Adding the directory to them would cause "can be a part of only one module" errors.
        val commonTestDir = File(buildDir, "generated/fakt/commonTest/kotlin")

        // Only register if commonTest exists in the project
        val hasCommonTest = kotlin.sourceSets.findByName("commonTest") != null

        if (hasCommonTest) {
            // Collect all test source sets first to determine which are intermediate
            val allTestSourceSets = kotlin.sourceSets.filter { it.name.endsWith("Test") }

            allTestSourceSets.forEach { sourceSet ->
                // Skip commonTest itself
                if (sourceSet.name == "commonTest") return@forEach

                // Check if this is an intermediate source set by seeing if any other test
                // source set depends on it (making it a parent in the hierarchy)
                val isIntermediateSourceSet =
                    allTestSourceSets.any { otherSourceSet ->
                        otherSourceSet.name != sourceSet.name &&
                            otherSourceSet.dependsOn.contains(sourceSet)
                    }

                // Only add commonTest dir to leaf source sets (not intermediate ones)
                if (!isIntermediateSourceSet) {
                    sourceSet.kotlin.srcDir(commonTestDir)
                    project.logger.info(
                        "Fakt: Added commonTest generated dir to ${sourceSet.name}: $commonTestDir",
                    )
                } else {
                    project.logger.debug(
                        "Fakt: Skipped adding commonTest dir to intermediate source set: ${sourceSet.name}",
                    )
                }
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
}
