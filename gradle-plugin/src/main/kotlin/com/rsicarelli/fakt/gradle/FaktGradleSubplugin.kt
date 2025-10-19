// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalFaktMultiModule::class)

package com.rsicarelli.fakt.gradle

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.util.Base64

/**
 * Gradle plugin for Fakt compiler plugin.
 *
 * This plugin:
 * 1. Registers the Fakt compiler plugin with Kotlin compilation
 * 2. Provides the `fakt { }` DSL for configuration
 * 3. Automatically adds runtime dependency to test configurations
 * 4. Configures the plugin for test-only code generation
 */
@Suppress("unused") // used by reflection
class FaktGradleSubplugin : KotlinCompilerPluginSupportPlugin {
    companion object {
        const val PLUGIN_ID = "com.rsicarelli.fakt"
        const val PLUGIN_ARTIFACT_NAME = "compiler"
        const val PLUGIN_GROUP_ID = "com.rsicarelli.fakt"
        const val PLUGIN_VERSION = "1.0.0-SNAPSHOT" // Using project version
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

        // Add runtime dependency to test configurations (both modes need this)
        addRuntimeDependencies(target)

        target.logger.info("Fakt: Applied Gradle plugin to project ${target.name}")
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        val project = kotlinCompilation.project
        val extension = project.extensions.findByType(FaktPluginExtension::class.java)

        // Skip compiler plugin in collector mode
        if (extension?.collectFrom?.isPresent == true) {
            project.logger.info(
                "Fakt: Skipping compiler plugin for '${kotlinCompilation.name}' (collector mode)",
            )
            return false
        }

        // Apply to main compilations where @Fake annotations are defined
        // - JVM/Android projects: "main" compilation
        // - KMP projects: "jvmMain", "jsMain", "iosMain", "commonMain", etc.
        val compilationName = kotlinCompilation.name.lowercase()

        val isMainCompilation =
            compilationName == "main" ||
                compilationName.endsWith("main")

        project.logger.info(
            "Fakt: Checking compilation '${kotlinCompilation.name}' - applicable: $isMainCompilation",
        )

        return isMainCompilation
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(
            groupId = PLUGIN_GROUP_ID,
            artifactId = PLUGIN_ARTIFACT_NAME,
            version = PLUGIN_VERSION,
        )

    override fun applyToCompilation(kotlinCompilation: KotlinCompilation<*>): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.project
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        project.logger.info("Fakt: Applying compiler plugin to compilation ${kotlinCompilation.name}")

        return project.provider {
            buildList {
                // Pass configuration options to the compiler plugin
                add(SubpluginOption(key = "enabled", value = extension.enabled.get().toString()))
                add(SubpluginOption(key = "debug", value = extension.debug.get().toString()))

                // Build complete source set context using modern API
                val buildDir =
                    project.layout.buildDirectory
                        .get()
                        .asFile.absolutePath
                val context = SourceSetDiscovery.buildContext(kotlinCompilation, buildDir)

                // Serialize context to Base64-encoded JSON for compiler plugin
                val json = Json { prettyPrint = false }
                val jsonString = json.encodeToString(context)
                val base64Encoded = Base64.getEncoder().encodeToString(jsonString.toByteArray())
                add(SubpluginOption(key = "sourceSetContext", value = base64Encoded))

                // Also pass output directory for backwards compatibility
                add(SubpluginOption(key = "outputDir", value = context.outputDirectory))

                if (extension.debug.get()) {
                    project.logger.info(
                        """
                        |Fakt: Source Set Context for ${kotlinCompilation.name}:
                        |  Target: ${context.targetName} (${context.platformType})
                        |  Default Source Set: ${context.defaultSourceSet.name}
                        |  All Source Sets: ${context.allSourceSets.joinToString { it.name }}
                        |  Is Test: ${context.isTest}
                        |  Output: ${context.outputDirectory}
                        """.trimMargin(),
                    )
                }

                project.logger.info("Fakt: Configured compiler plugin with $size options")
            }
        }
    }

    /**
     * Add runtime dependency to test configurations automatically.
     */
    private fun addRuntimeDependencies(project: Project) {
        // Add runtime dependency to all test-related configurations
        project.configurations.configureEach { configuration ->
            val configName = configuration.name.lowercase()

            if (configName.contains("test") &&
                (configName.endsWith("implementation") || configName.endsWith("api"))
            ) {
                project.dependencies.add(
                    configuration.name,
                    "${PLUGIN_GROUP_ID}:runtime:${PLUGIN_VERSION}",
                )

                project.logger.info("Fakt: Added runtime dependency to configuration ${configuration.name}")
            }
        }
    }
}
