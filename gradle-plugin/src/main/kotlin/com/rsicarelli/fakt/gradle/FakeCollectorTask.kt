// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Task to collect generated fakes from a source project.
 *
 * This task copies generated fake implementations from a source project's
 * build/generated/fakt directory to this project's source directories,
 * enabling the dedicated fake module pattern.
 *
 * Example usage:
 * ```
 * foundation/              # Generates fakes
 * foundation-fakes/        # Collects fakes (this task runs here)
 * domain/                  # Uses fakes via testImplementation(foundation-fakes)
 * ```
 */
abstract class FakeCollectorTask : DefaultTask() {

    /**
     * The path to the source project that generates fakes.
     * Configuration cache compatible (stores path, not Project object).
     */
    @get:Input
    @get:Optional
    abstract val sourceProjectPath: Property<String>

    /**
     * The directory where the source project generates fakes.
     * Typically: build/generated/fakt/
     */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceGeneratedDir: DirectoryProperty

    /**
     * The destination directory where collected fakes will be placed.
     * Typically: src/commonMain/kotlin/ or build/generated/collected-fakes/
     */
    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    init {
        group = "fakt"
        description = "Collects generated fakes from source project"

        // Task depends on source project's compilation to ensure fakes are generated first
        // Note: Dependency will be configured in registerForKmpProject to avoid configuration cache issues
    }

    @TaskAction
    fun collectFakes() {
        val srcDir = sourceGeneratedDir.asFile.get()
        val destDir = destinationDir.asFile.get()

        if (!srcDir.exists()) {
            logger.warn(
                "Fakt: Source directory does not exist: $srcDir. " +
                    "Make sure the source project generates fakes first."
            )
            return
        }

        // Create destination directory if it doesn't exist
        destDir.mkdirs()

        // Copy all generated fake files
        var copiedCount = 0
        srcDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { sourceFile ->
                val relativePath = sourceFile.relativeTo(srcDir)
                val destFile = destDir.resolve(relativePath)

                // Create parent directories
                destFile.parentFile.mkdirs()

                // Copy file
                sourceFile.copyTo(destFile, overwrite = true)
                copiedCount++

                logger.info("Fakt: Collected fake: ${relativePath.path}")
            }

        val srcProjectName = sourceProjectPath.orNull?.substringAfterLast(":") ?: "unknown"
        logger.lifecycle("Fakt: Collected $copiedCount fake(s) from $srcProjectName")
    }

    companion object {
        /**
         * Register collector tasks for a KMP project.
         *
         * This creates tasks for each source set in the target project,
         * mapping them to corresponding source sets in the source project.
         *
         * @param project The target project (collector module)
         * @param extension The Fakt plugin extension
         */
        fun registerForKmpProject(
            project: Project,
            extension: FaktPluginExtension
        ) {
            val srcProject = extension.collectFrom.orNull ?: return

            val kotlinExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            if (kotlinExtension == null) {
                // For non-KMP projects, create a single collector task
                registerSingleCollectorTask(project, extension)
                return
            }

            // For KMP projects, map source sets
            val sourceSetMappings = mapOf(
                "commonMain" to "fakes",
                "jvmMain" to "jvmFakes",
                "jsMain" to "jsFakes",
                "iosArm64Main" to "iosArm64Fakes"
                // Add more as needed
            )

            sourceSetMappings.forEach { (targetSourceSet, generatedSubdir) ->
                // Check if this source set exists in the project
                val sourceSet = kotlinExtension.sourceSets.findByName(targetSourceSet) ?: return@forEach

                val taskName = "collect${targetSourceSet.replaceFirstChar { it.uppercase() }}Fakes"
                val task = project.tasks.register(taskName, FakeCollectorTask::class.java) {
                    it.sourceProjectPath.set(srcProject.path)
                    it.sourceGeneratedDir.set(
                        srcProject.layout.buildDirectory.dir("generated/fakt/$generatedSubdir/kotlin")
                    )
                    it.destinationDir.set(
                        project.layout.buildDirectory.dir("generated/collected-fakes/$targetSourceSet/kotlin")
                    )

                    // Add dependency on source project's compilation tasks
                    it.dependsOn(srcProject.tasks.matching { task ->
                        task.name.contains("compile", ignoreCase = true)
                    })
                }

                // Add collected directory to source set
                sourceSet.kotlin.srcDir(task.map { it.destinationDir })

                project.logger.info(
                    "Fakt: Registered collector task '$taskName' for source set '$targetSourceSet'"
                )
            }
        }

        /**
         * Register a single collector task for non-KMP projects.
         */
        private fun registerSingleCollectorTask(
            project: Project,
            extension: FaktPluginExtension
        ) {
            val srcProject = extension.collectFrom.orNull ?: return

            project.tasks.register("collectFakes", FakeCollectorTask::class.java) {
                it.sourceProjectPath.set(srcProject.path)
                it.sourceGeneratedDir.set(
                    srcProject.layout.buildDirectory.dir("generated/fakt/test/kotlin")
                )
                it.destinationDir.set(
                    project.layout.buildDirectory.dir("generated/collected-fakes/kotlin")
                )

                // Add dependency on source project's compilation tasks
                it.dependsOn(srcProject.tasks.matching { task ->
                    task.name.contains("compile", ignoreCase = true)
                })
            }

            project.logger.info("Fakt: Registered single collector task 'collectFakes'")
        }
    }
}
