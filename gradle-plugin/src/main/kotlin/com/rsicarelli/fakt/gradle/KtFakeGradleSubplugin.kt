// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.io.File

/**
 * Gradle plugin for KtFakes compiler plugin.
 *
 * This plugin:
 * 1. Registers the KtFakes compiler plugin with Kotlin compilation
 * 2. Provides the `ktfake { }` DSL for configuration
 * 3. Automatically adds runtime dependency to test configurations
 * 4. Configures the plugin for test-only code generation
 */
class FaktGradleSubplugin : KotlinCompilerPluginSupportPlugin {

    companion object {
        const val PLUGIN_ID = "com.rsicarelli.fakt"
        const val PLUGIN_ARTIFACT_NAME = "compiler"
        const val PLUGIN_GROUP_ID = "com.rsicarelli.fakt"
        const val PLUGIN_VERSION = "1.0.0-SNAPSHOT" // Using project version
    }

    override fun apply(target: Project) {
        // Create the ktfake extension for configuration
        val extension = target.extensions.create("ktfake", FaktPluginExtension::class.java)

        // Configure source sets automatically after project evaluation
        target.afterEvaluate {
            configureSourceSets(target)
        }

        // Add runtime dependency to test configurations
        addRuntimeDependencies(target)

        target.logger.info("KtFakes: Applied Gradle plugin to project ${target.name}")
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
        // Apply to main compilations where @Fake annotations are defined
        // - JVM/Android projects: "main" compilation
        // - KMP projects: "jvmMain", "jsMain", "iosMain", "commonMain", etc.
        val compilationName = kotlinCompilation.name.lowercase()

        val isMainCompilation = compilationName == "main" ||
                               compilationName.endsWith("main")

        kotlinCompilation.project.logger.info(
            "KtFakes: Checking compilation '${kotlinCompilation.name}' - applicable: $isMainCompilation"
        )

        return isMainCompilation
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
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        project.logger.info("KtFakes: Applying compiler plugin to compilation ${kotlinCompilation.name}")

        return project.provider {
            buildList {
                // Pass configuration options to the compiler plugin
                // Only pass options that the compiler plugin actually supports
                add(SubpluginOption(key = "enabled", value = extension.enabled.get().toString()))
                add(SubpluginOption(key = "debug", value = extension.debug.get().toString()))

                // Automatically configure output directory based on compilation type
                val outputDir = getGeneratedSourcesDirectory(project, kotlinCompilation)
                add(SubpluginOption(key = "outputDir", value = outputDir))

                project.logger.info("KtFakes: Configured compiler plugin with ${size} options")
            }
        }
    }

    /**
     * Automatically configure source sets for multiplatform projects.
     * This ensures generated fakes are accessible from test source sets.
     */
    private fun configureSourceSets(project: Project) {
        // Check if this is a multiplatform project
        val kotlinExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        if (kotlinExtension != null) {
            configureMultiplatformSourceSets(project, kotlinExtension)
        } else {
            // For single-platform projects, configure based on applied plugins
            configureJvmOnlySourceSets(project)
        }
    }

    /**
     * Configure multiplatform source sets to include generated fakes.
     */
    private fun configureMultiplatformSourceSets(project: Project, kotlin: KotlinMultiplatformExtension) {
        kotlin.sourceSets.configureEach { sourceSet ->
            if (sourceSet.name.endsWith("Test")) {
                val generatedDir = getGeneratedSourcesDirectoryForSourceSet(project, sourceSet.name)
                sourceSet.kotlin.srcDir(generatedDir)

                project.logger.info("KtFakes: Configured source set '${sourceSet.name}' to include generated sources from: $generatedDir")
            }
        }
    }

    /**
     * Configure JVM-only projects.
     */
    private fun configureJvmOnlySourceSets(project: Project) {
        // For JVM-only projects, add generated sources to test source sets
        project.tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) { task ->
            if (task.name.contains("Test", ignoreCase = true)) {
                val generatedDir = File(project.buildDir, "generated/ktfake/test/kotlin")
                task.source(generatedDir)

                project.logger.info("KtFakes: Configured test compilation task '${task.name}' to include generated sources")
            }
        }
    }

    /**
     * Get the appropriate generated sources directory for a compilation.
     *
     * Since we generate fakes FROM main sources FOR test usage, we always output to test directories.
     * For KMP projects with shared code, we generate to common/test/kotlin for maximum compatibility.
     */
    private fun getGeneratedSourcesDirectory(project: Project, compilation: KotlinCompilation<*>): String {
        val compilationName = compilation.name
        val targetName = compilation.target.name

        // For KMP projects, check if this is processing commonMain or metadata
        if (compilationName.equals("commonMain", ignoreCase = true) || targetName == "metadata") {
            return File(project.buildDir, "generated/ktfake/common/test/kotlin").absolutePath
        }

        // Check if this is a KMP project with commonTest source set
        // If so, generate to common/test for all targets to share
        val kotlinExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
        if (kotlinExtension != null) {
            // This is a KMP project - check if commonTest exists
            val hasCommonTest = kotlinExtension.sourceSets.findByName("commonTest") != null
            if (hasCommonTest) {
                // Generate to common/test so all platform tests can access
                return File(project.buildDir, "generated/ktfake/common/test/kotlin").absolutePath
            }
        }

        // Map main compilation names to test directories
        // main → test, jvmMain → jvmTest, iosMain → iosTest, etc.
        val testDirName = when {
            compilationName.equals("main", ignoreCase = true) -> "test"
            compilationName.endsWith("Main", ignoreCase = true) ->
                compilationName.removeSuffix("Main").removeSuffix("main") + "Test"
            else -> compilationName
        }

        // For specific platform targets in KMP: build/generated/ktfake/{targetName}/test/kotlin
        // For single-platform JVM: build/generated/ktfake/test/kotlin (targetName="jvm", compilationName="main")
        val directory = File(project.buildDir, "generated/ktfake/$targetName/$testDirName/kotlin")

        return directory.absolutePath
    }

    /**
     * Get generated sources directory for a specific source set.
     */
    private fun getGeneratedSourcesDirectoryForSourceSet(project: Project, sourceSetName: String): File {
        return when {
            sourceSetName == "commonTest" -> File(project.buildDir, "generated/ktfake/common/test/kotlin")
            sourceSetName.endsWith("Test") -> {
                val target = sourceSetName.removeSuffix("Test")
                File(project.buildDir, "generated/ktfake/$target/test/kotlin")
            }
            else -> File(project.buildDir, "generated/ktfake/common/test/kotlin")
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
                    "${PLUGIN_GROUP_ID}:runtime:${PLUGIN_VERSION}"
                )

                project.logger.info("KtFakes: Added runtime dependency to configuration ${configuration.name}")
            }
        }
    }
}
