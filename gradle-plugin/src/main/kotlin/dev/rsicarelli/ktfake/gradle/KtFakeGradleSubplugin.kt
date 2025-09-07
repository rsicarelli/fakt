// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

/**
 * Gradle plugin for KtFakes compiler plugin.
 *
 * This plugin:
 * 1. Registers the KtFakes compiler plugin with Kotlin compilation
 * 2. Provides the `ktfake { }` DSL for configuration
 * 3. Automatically adds runtime dependency to test configurations
 * 4. Configures the plugin for test-only code generation
 */
class KtFakeGradleSubplugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val PLUGIN_ID = "dev.rsicarelli.ktfake"
        const val PLUGIN_ARTIFACT_NAME = "ktfake-compiler"
        const val PLUGIN_GROUP_ID = "dev.rsicarelli.ktfake"
        const val PLUGIN_VERSION = "0.1.0-SNAPSHOT" // Will be replaced with actual version
    }

    override fun apply(target: Project) {
        // Create the ktfake extension for configuration
        val extension = target.extensions.create("ktfake", KtFakePluginExtension::class.java)

        // Add runtime dependency to test configurations
        addRuntimeDependencies(target)

        target.logger.info("KtFakes: Applied Gradle plugin to project ${target.name}")
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        // Only apply to compilations that include test source sets
        // This ensures fakes are only generated in test contexts
        val compilationName = kotlinCompilation.name.lowercase()

        val isTestCompilation = compilationName.contains("test") ||
                               compilationName.contains("androidtest") ||
                               compilationName.contains("jvmtest") ||
                               compilationName.contains("commontest")

        kotlinCompilation.project.logger.info(
            "KtFakes: Checking compilation '${kotlinCompilation.name}' - applicable: $isTestCompilation"
        )

        return isTestCompilation
    }

    override fun getCompilerPluginId(): String = PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = PLUGIN_GROUP_ID,
        artifactId = PLUGIN_ARTIFACT_NAME,
        version = PLUGIN_VERSION
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.project
        val extension = project.extensions.getByType(KtFakePluginExtension::class.java)

        project.logger.info("KtFakes: Applying compiler plugin to compilation ${kotlinCompilation.name}")

        return project.provider {
            buildList {
                // Pass configuration options to the compiler plugin
                add(SubpluginOption(key = "enabled", value = extension.enabled.toString()))
                add(SubpluginOption(key = "debug", value = extension.debug.toString()))

                if (extension.reportsDestination.isPresent) {
                    add(SubpluginOption(
                        key = "reportsDestination",
                        value = extension.reportsDestination.get().asFile.absolutePath
                    ))
                }

                add(SubpluginOption(key = "generateCallTracking", value = extension.generateCallTracking.toString()))
                add(SubpluginOption(key = "generateBuilderPatterns", value = extension.generateBuilderPatterns.toString()))
                add(SubpluginOption(key = "threadSafetyChecks", value = extension.threadSafetyChecks.toString()))

                project.logger.info("KtFakes: Configured compiler plugin with ${size} options")
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
                (configName.endsWith("implementation") || configName.endsWith("api"))) {

                project.dependencies.add(
                    configuration.name,
                    "${PLUGIN_GROUP_ID}:ktfake-runtime:${PLUGIN_VERSION}"
                )

                project.logger.info("KtFakes: Added runtime dependency to configuration ${configuration.name}")
            }
        }
    }
}
