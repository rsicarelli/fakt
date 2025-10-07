// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
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
     * Optional because not all source sets may have generated fakes.
     */
    @get:Internal // Not using @InputDirectory to allow missing directories
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
        val faktRootDir = sourceGeneratedDir.asFile.get()
        val destDir = destinationDir.asFile.get()

        if (!faktRootDir.exists()) {
            logger.warn(
                "Fakt: Source directory does not exist: $faktRootDir. " +
                    "Make sure the source project generates fakes first.",
            )
            return
        }

        // Auto-discover all source set directories (commonTest, jvmTest, etc.)
        // This avoids hardcoded mappings and works with any KMP configuration
        val sourceSetDirs = faktRootDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

        if (sourceSetDirs.isEmpty()) {
            logger.warn("Fakt: No generated fakes found in $faktRootDir")
            return
        }

        // Create destination directory if it doesn't exist
        destDir.mkdirs()

        // Collect from all discovered source set directories
        var copiedCount = 0
        sourceSetDirs.forEach { sourceSetDir ->
            sourceSetDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { sourceFile ->
                    val relativePath = sourceFile.relativeTo(sourceSetDir)
                    val destFile = destDir.resolve(relativePath)

                    // Create parent directories
                    destFile.parentFile.mkdirs()

                    // Copy file
                    sourceFile.copyTo(destFile, overwrite = true)
                    copiedCount++

                    logger.info("Fakt: Collected fake: ${relativePath.path} from ${sourceSetDir.name}")
                }
        }

        val srcProjectName = sourceProjectPath.orNull?.substringAfterLast(":") ?: "unknown"
        logger.lifecycle("Fakt: Collected $copiedCount fake(s) from $srcProjectName")
    }

    companion object {
        /**
         * Register collector task for a KMP project.
         *
         * This creates a single task that auto-discovers all generated fakes
         * from the source project's build/generated/fakt directory, avoiding
         * hardcoded source set mappings.
         *
         * The task scans all subdirectories (commonTest, jvmTest, etc.) and
         * collects all generated fakes into the collector module's commonMain,
         * making them available to all platforms.
         *
         * @param project The target project (collector module)
         * @param extension The Fakt plugin extension
         */
        fun registerForKmpProject(
            project: Project,
            extension: FaktPluginExtension,
        ) {
            val srcProject = extension.collectFrom.orNull ?: return

            val kotlinExtension = project.extensions.findByType(KotlinMultiplatformExtension::class.java)
            if (kotlinExtension == null) {
                // For non-KMP projects, create a single collector task
                registerSingleCollectorTask(project, extension)
                return
            }

            // Create single collector task that auto-discovers all generated fakes
            // No hardcoded source set mappings - works with any KMP configuration
            val taskName = "collectCommonMainFakes"
            val task =
                project.tasks.register(taskName, FakeCollectorTask::class.java) {
                    it.sourceProjectPath.set(srcProject.path)

                    // Point to root fakt directory - task will auto-discover subdirectories
                    it.sourceGeneratedDir.set(
                        srcProject.layout.buildDirectory.dir("generated/fakt"),
                    )

                    // Collect to commonMain so all platforms can access the fakes
                    it.destinationDir.set(
                        project.layout.buildDirectory.dir("generated/collected-fakes/commonMain/kotlin"),
                    )

                    // Add dependency on source project's MAIN compilation tasks only
                    // Avoid test compilations to prevent circular dependencies
                    // (test compilations may depend on -fakes modules)
                    it.dependsOn(
                        srcProject.tasks.matching { task ->
                            task.name.contains("compile", ignoreCase = true) &&
                                !task.name.contains("test", ignoreCase = true)
                        },
                    )
                }

            // Add collected directory to commonMain source set
            val commonMain = kotlinExtension.sourceSets.getByName("commonMain")
            commonMain.kotlin.srcDir(task.map { it.destinationDir })

            project.logger.info(
                "Fakt: Registered dynamic collector task '$taskName' (auto-discovers all generated fakes)",
            )
        }

        /**
         * Register a single collector task for non-KMP projects.
         *
         * Uses the same auto-discovery approach as KMP projects for consistency.
         */
        private fun registerSingleCollectorTask(
            project: Project,
            extension: FaktPluginExtension,
        ) {
            val srcProject = extension.collectFrom.orNull ?: return

            project.tasks.register("collectFakes", FakeCollectorTask::class.java) {
                it.sourceProjectPath.set(srcProject.path)

                // Point to root fakt directory - task will auto-discover subdirectories
                it.sourceGeneratedDir.set(
                    srcProject.layout.buildDirectory.dir("generated/fakt"),
                )

                it.destinationDir.set(
                    project.layout.buildDirectory.dir("generated/collected-fakes/kotlin"),
                )

                // Add dependency on source project's MAIN compilation tasks only
                // Avoid test compilations to prevent circular dependencies
                // (test compilations may depend on -fakes modules)
                it.dependsOn(
                    srcProject.tasks.matching { task ->
                        task.name.contains("compile", ignoreCase = true) &&
                            !task.name.contains("test", ignoreCase = true)
                    },
                )
            }

            project.logger.info("Fakt: Registered dynamic collector task 'collectFakes' (auto-discovers all generated fakes)")
        }
    }
}
