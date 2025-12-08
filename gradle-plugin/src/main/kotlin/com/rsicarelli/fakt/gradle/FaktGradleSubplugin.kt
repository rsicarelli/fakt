// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalFaktMultiModule::class)

package com.rsicarelli.fakt.gradle

import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.util.Base64

/**
 * Gradle plugin for Fakt compiler plugin integration.
 *
 * This is the main entry point that bridges Gradle build system with the Fakt compiler plugin.
 * It implements [KotlinCompilerPluginSupportPlugin] to hook into Kotlin's compilation lifecycle.
 *
 * ## Plugin Lifecycle
 *
 * ```
 * 1. apply(Project)
 *    └─> Creates `fakt { }` extension
 *    └─> Configures source sets (generator mode) OR registers tasks (collector mode)
 *    └─> Adds runtime dependencies to test configurations
 *
 * 2. isApplicable(KotlinCompilation)
 *    └─> Called for each compilation (main, test, jvmMain, etc.)
 *    └─> Returns true for main compilations only (where @Fake annotations exist)
 *    └─> Skips test compilations (generated code goes there, not analyzed)
 *
 * 3. applyToCompilation(KotlinCompilation)
 *    └─> Called for compilations where isApplicable returned true
 *    └─> Serializes configuration to compiler plugin options
 *    └─> Passes source set context (output directories, hierarchy, etc.)
 * ```
 *
 * ## Modes of Operation
 *
 * **Generator Mode (default):**
 * ```kotlin
 * // build.gradle.kts
 * fakt {
 *     enabled.set(true)
 *     logLevel.set(LogLevel.INFO)
 * }
 * // Generates fakes from @Fake annotations in main source sets
 * ```
 *
 * **Collector Mode (experimental):**
 * ```kotlin
 * // build.gradle.kts
 * fakt {
 *     collectFrom(project(":source-module"))
 * }
 * // Copies generated fakes from another module without compilation
 * ```
 *
 * ## Integration Points
 *
 * - **Extension DSL**: [FaktPluginExtension] provides `fakt { }` block
 * - **Compiler Plugin**: Serializes options to Fakt compiler plugin
 * - **Source Sets**: [SourceSetConfigurator] adds generated directories to test source sets
 * - **Multi-Module**: [FakeCollectorTask] handles cross-module fake collection
 *
 * @see FaktPluginExtension
 * @see SourceSetDiscovery
 * @see FakeCollectorTask
 */
@Suppress("unused") // used by reflection
public class FaktGradleSubplugin : KotlinCompilerPluginSupportPlugin {
    public companion object {
        public const val PLUGIN_ID: String = "com.rsicarelli.fakt"
        public const val PLUGIN_ARTIFACT_NAME: String = "fakt-compiler"
        public const val PLUGIN_GROUP_ID: String = "com.rsicarelli"
        public const val PLUGIN_VERSION: String = "1.0.0-alpha01"
    }

    @OptIn(ExperimentalFaktMultiModule::class)
    override fun apply(target: Project) {
        // Create the fakt extension for configuration
        val extension = target.extensions.create("fakt", FaktPluginExtension::class.java)

        // Determine mode after project evaluation
        target.afterEvaluate {
            val isCollectorMode = extension.collectFrom.isPresent

            if (isCollectorMode) {
                // COLLECTOR MODE: Collect fakes from another project
                val sourceProject = extension.collectFrom.get()
                target.logger.info(
                    "Fakt: Collector mode enabled - collecting fakes from ${sourceProject.name}",
                )

                // Register collector tasks (handles KMP automatically)
                FakeCollectorTask.registerForKmpProject(target, extension)
            } else {
                // GENERATOR MODE: Generate fakes from @Fake annotations
                target.logger.info("Fakt: Generator mode enabled - generating fakes")

                // Configure source sets for generated code
                val configurator = SourceSetConfigurator(target)
                configurator.configureSourceSets()
            }
        }

        target.logger.info("Fakt: Applied Gradle plugin to project ${target.name}")
    }

    /**
     * Determines if Fakt compiler plugin should be applied to a specific compilation.
     *
     * This is called by Gradle for EVERY Kotlin compilation in the project (main, test, jvmMain,
     * jvmTest, commonMain, commonTest, etc.). We only want to analyze main compilations where
     * `@Fake` annotations are defined, NOT test compilations where generated fakes are used.
     *
     * ## Decision Logic
     *
     * **Skip if collector mode** (no compilation needed, just copy tasks):
     * ```kotlin
     * fakt { collectFrom(project(":source")) } → returns false
     * ```
     *
     * **Apply to main compilations only:**
     * ```
     * Single-platform JVM:
     *   "main" → true  ✅
     *   "test" → false ❌
     *
     * KMP:
     *   "metadata" → true ✅ (commonMain representation)
     *   "commonMain" → true ✅
     *   "jvmMain" → true ✅
     *   "iosMain" → true ✅
     *   "commonTest" → false ❌
     *   "jvmTest" → false ❌
     * ```
     *
     * ## Why Skip Test Compilations?
     *
     * Test compilations don't contain `@Fake` annotations to process. They only USE the
     * generated fakes that were created from main source sets. Applying the plugin to test
     * compilations would:
     * - Waste compilation time
     * - Generate duplicate/empty output
     * - Cause circular dependencies
     *
     * @param kotlinCompilation The Kotlin compilation to check
     * @return `true` if plugin should be applied, `false` to skip this compilation
     * @see applyToCompilation
     */
    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.project
        val extension = project.extensions.findByType(FaktPluginExtension::class.java)

        if (extension == null) return false

        if (extension.collectFrom.isPresent) {
            project.logger.info(
                "Fakt: Skipping compiler plugin for '${kotlinCompilation.name}' (collector mode)",
            )
            return false
        }

        val compilationName = kotlinCompilation.name.lowercase()

        return compilationName == "main" ||
                compilationName.endsWith("main") ||
                compilationName == "metadata"
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = PLUGIN_GROUP_ID,
            artifactId = PLUGIN_ARTIFACT_NAME,
            version = PLUGIN_VERSION,
        )

    /**
     * Applies Fakt compiler plugin to a specific Kotlin compilation.
     *
     * This is called by Gradle for each compilation where [isApplicable] returned true.
     * It serializes all configuration and metadata into compiler plugin options that
     * are passed to the Fakt compiler plugin via command-line arguments.
     *
     * ## Serialization Strategy
     *
     * 1. **Configuration Options**: Direct string/boolean values
     *    - `enabled`: true/false
     *    - `logLevel`: INFO/DEBUG/TRACE/QUIET
     *
     * 2. **Source Set Context**: Base64-encoded JSON
     *    - Contains: compilation metadata, source set hierarchy, output directories
     *    - Serialized with kotlinx.serialization
     *    - Encoded to avoid special character issues in command-line arguments
     *
     * ## Example Compiler Options
     *
     * ```
     * -P plugin:com.rsicarelli.fakt:enabled=true
     * -P plugin:com.rsicarelli.fakt:logLevel=INFO
     * -P plugin:com.rsicarelli.fakt:sourceSetContext={hash}
     * -P plugin:com.rsicarelli.fakt:outputDir=/path/to/build/generated/fakt/test/kotlin
     * ```
     *
     * @param kotlinCompilation The Kotlin compilation to configure (e.g., jvmMain, commonMain)
     * @return A [Provider] of compiler plugin options, evaluated lazily at configuration time
     * @see SourceSetDiscovery.buildContext
     * @see FaktPluginExtension
     */
    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.project
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        project.logger.info("Fakt: Applying compiler plugin to compilation ${kotlinCompilation.name}")

        return project.provider {
            buildList {
                // Pass configuration options to the compiler plugin
                add(SubpluginOption(key = "enabled", value = extension.enabled.get().toString()))
                add(SubpluginOption(key = "logLevel", value = extension.logLevel.get().name))

                val buildDir = project.layout.buildDirectory.get().asFile.absolutePath
                val context = SourceSetDiscovery.buildContext(kotlinCompilation, buildDir)

                // Serialize context to Base64-encoded JSON for compiler plugin
                val json = Json { prettyPrint = false }
                val jsonString = json.encodeToString(context)
                val base64Encoded = Base64.getEncoder().encodeToString(jsonString.toByteArray())
                add(SubpluginOption(key = "sourceSetContext", value = base64Encoded))

                // Also pass output directory for backwards compatibility
                add(SubpluginOption(key = "outputDir", value = context.outputDirectory))

                project.logger.info("Fakt: Configured compiler plugin with $size options")
            }
        }
    }
}
