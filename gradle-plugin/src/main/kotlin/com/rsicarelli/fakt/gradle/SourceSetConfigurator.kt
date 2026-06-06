// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import java.io.File
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile

/**
 * Configures source sets and output directories for generated fake implementations.
 *
 * This internal utility handles the complex task of determining where to place generated fakes and
 * ensuring they're accessible from test compilations. It supports both single-platform (JVM-only)
 * and Kotlin Multiplatform (KMP) projects.
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
 * This dual registration ensures platform tests can import fakes generated from common interfaces
 * while maintaining support for platform-specific fakes.
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
    private val useTestFixtures: Boolean = false,
) {
    /**
     * Automatically configure source sets for multiplatform projects. This ensures generated fakes
     * are accessible from test source sets.
     */
    fun configureSourceSets() =
        project.extensions
            .findByType(KotlinMultiplatformExtension::class.java)
            ?.let(::configureKmpSourceSets) ?: configureJvmSourceSets()

    /**
     * Configure multiplatform source sets to include generated fakes.
     *
     * This adds generated directories to EXISTING test source sets. Generated fakes are placed in
     * directories matching the test source set names:
     * - commonTest → build/generated/fakt/commonTest/kotlin
     * - jvmTest → build/generated/fakt/jvmTest/kotlin
     * - etc.
     */
    private fun configureKmpSourceSets(kotlin: KotlinMultiplatformExtension) {
        val buildDir = project.layout.buildDirectory.get().asFile

        // Add a per-source-set generated directory to every test source set.
        // Example: jvmTest → build/generated/fakt/jvmTest/kotlin
        kotlin.sourceSets.configureEach { sourceSet ->
            if (sourceSet.name.endsWith("Test")) {
                val generatedDir = File(buildDir, "generated/fakt/${sourceSet.name}/kotlin")
                sourceSet.kotlin.srcDir(generatedDir)
                project.logger.info("Fakt: Added generated dir to ${sourceSet.name}: $generatedDir")
            }
        }

        // Test source sets receive only their own generated directory. commonTest fakes reach
        // platform tests (jvmTest, iosX64Test, …) through KMP's compilation model, which
        // propagates compiled KLIBs through the source-set hierarchy. Adding commonTest's
        // directory to platform test source sets directly would compile the same files into
        // multiple modules and fail with "can be a part of only one module".
        val hasCommonTest = kotlin.sourceSets.findByName("commonTest") != null

        if (hasCommonTest) {
            project.logger.debug(
                "Fakt: relying on KMP dependency propagation for commonTest fake visibility"
            )
        }
    }

    /**
     * Configure JVM-only projects and Android projects.
     *
     * This method handles both standard JVM and Android compilation tasks by using
     * AbstractKotlinCompile, which is the parent class for all Kotlin compilation tasks.
     */
    private fun configureJvmSourceSets() {
        if (useTestFixtures) {
            configureTestFixturesSourceSet()
            return
        }

        // For JVM-only and Android projects, add generated sources to test source sets
        // Uses AbstractKotlinCompile to catch both JVM (KotlinCompile) and Android tasks
        project.tasks.withType(AbstractKotlinCompile::class.java) { task ->
            if (isTestTask(task.name)) {
                val generatedDir =
                    File(project.layout.buildDirectory.get().asFile, "generated/fakt/test/kotlin")

                task.source(generatedDir)

                project.logger.info(
                    "Fakt: Configured test compilation task '${task.name}' " +
                        "to include generated sources"
                )
            }
        }
    }

    /**
     * Configure testFixtures source set to include generated fakes.
     *
     * When `useGradleTestFixtures` is enabled, generated fakes go to
     * `build/generated/fakt/testFixtures/kotlin/` and are added to the `testFixtures` Java source
     * set. This makes fakes available to:
     * - The module's own tests (automatically via Gradle)
     * - Other modules via `testFixtures(project(":this-module"))` dependency
     */
    private fun configureTestFixturesSourceSet() {
        val generatedDir =
            File(project.layout.buildDirectory.get().asFile, "generated/fakt/testFixtures/kotlin")

        // Add generated directory to the testFixtures Java source set
        project.extensions
            .findByType(org.gradle.api.plugins.JavaPluginExtension::class.java)
            ?.sourceSets
            ?.findByName("testFixtures")
            ?.java
            ?.srcDir(generatedDir)

        project.logger.info(
            "Fakt: Configured testFixtures source set to include generated sources at $generatedDir"
        )
    }

    /** Matches unit and instrumented test compilations across JVM, Android, and KMP task names. */
    private fun isTestTask(taskName: String): Boolean {
        val normalized = taskName.lowercase()
        return normalized.contains("test")
    }
}
