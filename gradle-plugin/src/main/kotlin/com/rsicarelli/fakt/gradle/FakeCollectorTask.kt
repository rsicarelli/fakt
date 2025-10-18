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
import java.io.File

/**
 * Task to collect generated fakes from a source project with platform-aware placement.
 *
 * This task intelligently analyzes package structure to determine target platform
 * and places fakes in appropriate source sets:
 * - `api.jvm.*` packages → `jvmMain/kotlin/`
 * - `api.ios.*` packages → `iosMain/kotlin/`
 * - `api.shared.*` or `api.common.*` packages → `commonMain/kotlin/`
 *
 * This solves the cross-platform compilation problem where JVM-only interfaces
 * would fail to compile in commonMain.
 *
 * Example usage:
 * ```
 * foundation/              # Generates fakes for JVM + common interfaces
 *   ├── jvmMain/kotlin/    (JVM-specific: DatabaseService)
 *   └── commonMain/kotlin/ (Common: NetworkService)
 *
 * foundation-fakes/        # Collects with platform detection
 *   ├── jvmMain/kotlin/foundation/jvm/FakeDatabaseServiceImpl.kt
 *   └── commonMain/kotlin/foundation/shared/FakeNetworkServiceImpl.kt
 *
 * domain/                  # Uses fakes via implementation(foundation-fakes)
 *   └── Can access both JVM and common fakes correctly
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

        if (!faktRootDir.exists()) {
            logger.warn(
                "Fakt: Source directory does not exist: $faktRootDir. " +
                    "Make sure the source project generates fakes first.",
            )
            return
        }

        // Auto-discover all source set directories (commonTest, jvmTest, etc.)
        val sourceSetDirs = faktRootDir.listFiles()?.filter { it.isDirectory } ?: emptyList()

        if (sourceSetDirs.isEmpty()) {
            logger.warn("Fakt: No generated fakes found in $faktRootDir")
            return
        }

        // Destination base directory (parent of platform-specific dirs)
        // destinationDir points to a placeholder, we use its parent
        val destinationBaseDir = destinationDir.asFile.get().parentFile

        var totalCollected = 0
        val platformStats = mutableMapOf<String, Int>()

        // Process each source set directory with platform detection
        sourceSetDirs.forEach { sourceSetDir ->
            val kotlinDir = sourceSetDir.resolve("kotlin")
            if (!kotlinDir.exists() || !kotlinDir.isDirectory) {
                logger.info("Fakt: Skipping ${sourceSetDir.name} (no kotlin directory)")
                return@forEach
            }

            // Use platform detection for this source set
            val result =
                collectWithPlatformDetection(
                    sourceDir = kotlinDir,
                    destinationBaseDir = destinationBaseDir,
                )

            totalCollected += result.collectedCount
            result.platformDistribution.forEach { (platform, count) ->
                platformStats[platform] = (platformStats[platform] ?: 0) + count
            }

            logger.info(
                "Fakt: Collected ${result.collectedCount} fake(s) from ${sourceSetDir.name}",
            )
        }

        // Log summary
        val srcProjectName = sourceProjectPath.orNull?.substringAfterLast(":") ?: "unknown"
        logger.lifecycle("Fakt: Collected $totalCollected fake(s) from $srcProjectName")
        platformStats.forEach { (platform, count) ->
            logger.lifecycle("  - $platform: $count file(s)")
        }
    }

    companion object {
        private const val PACKAGE_SCAN_LINES = 10 // Number of lines to scan for package declaration

        /**
         * Determines the appropriate platform source set based on file content.
         * Analyzes the package declaration to detect platform-specific markers.
         *
         * Detection strategy:
         * 1. Extract package declaration from file content
         * 2. Look for platform markers in package segments (.jvm., .js., .ios., etc.)
         * 3. Return appropriate source set or default to commonMain
         *
         * @param fileContent The content of the fake file
         * @return The source set name (e.g., "jvmMain", "commonMain", "iosMain")
         */
        fun determinePlatformSourceSet(fileContent: String): String {
            // Extract package declaration (first N lines for performance)
            val packageDeclaration =
                fileContent
                    .lines()
                    .take(PACKAGE_SCAN_LINES)
                    .firstOrNull { it.trim().startsWith("package ") }
                    ?.removePrefix("package ")
                    ?.trim()
                    ?: return "commonMain" // No package → commonMain

            // Split package into segments for analysis
            val packageSegments = packageDeclaration.split(".")

            // Detect platform markers in package segments
            return when {
                packageSegments.any { it == "jvm" } -> "jvmMain"
                packageSegments.any { it == "js" } -> "jsMain"
                packageSegments.any { it == "ios" } -> "iosMain"
                packageSegments.any { it == "native" } -> "nativeMain"
                packageSegments.any { it == "android" } -> "androidMain"
                packageSegments.any { it == "common" || it == "shared" } -> "commonMain"
                else -> "commonMain" // Default fallback
            }
        }

        /**
         * Result of collecting fakes with platform detection.
         *
         * @property collectedCount Total number of files collected
         * @property platformDistribution Map of platform → file count
         */
        data class CollectionResult(
            val collectedCount: Int,
            val platformDistribution: Map<String, Int>,
        )

        /**
         * Collects fakes from source directory with platform-specific placement.
         *
         * Scans the source directory for Kotlin files, detects their target platform
         * based on package structure, and places them in the appropriate platform
         * source set directory.
         *
         * @param sourceDir Directory containing generated fakes
         * @param destinationBaseDir Base directory for collected fakes
         * @return Collection result with statistics
         */
        fun collectWithPlatformDetection(
            sourceDir: File,
            destinationBaseDir: File,
        ): CollectionResult {
            val platformDistribution = mutableMapOf<String, Int>()
            var collectedCount = 0

            // Walk through all Kotlin files in source directory
            sourceDir
                .walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .forEach { sourceFile ->
                    // Read file content and detect platform
                    val fileContent = sourceFile.readText()
                    val platform = determinePlatformSourceSet(fileContent)

                    // Calculate relative path from source directory
                    val relativePath = sourceFile.relativeTo(sourceDir)

                    // Determine destination: {platform}/kotlin/{relativePath}
                    val destFile =
                        destinationBaseDir
                            .resolve("$platform/kotlin")
                            .resolve(relativePath)

                    // Create parent directories and copy file
                    destFile.parentFile.mkdirs()
                    sourceFile.copyTo(destFile, overwrite = true)

                    // Update statistics
                    collectedCount++
                    platformDistribution[platform] = (platformDistribution[platform] ?: 0) + 1
                }

            return CollectionResult(collectedCount, platformDistribution)
        }

        /**
         * Register collector task for a KMP project with platform-aware collection.
         *
         * This creates a single task that auto-discovers all generated fakes
         * and intelligently places them based on package structure:
         * - api.jvm.* → jvmMain/kotlin/
         * - api.ios.* → iosMain/kotlin/
         * - api.shared.* or api.common.* → commonMain/kotlin/
         *
         * Registers ALL *Main source sets (commonMain, jvmMain, iosMain, etc.)
         * so each platform can access its appropriate fakes.
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

            // Create single collector task with platform detection
            val taskName = "collectFakes"
            val task =
                project.tasks.register(taskName, FakeCollectorTask::class.java) {
                    it.sourceProjectPath.set(srcProject.path)

                    // Point to root fakt directory - task will auto-discover subdirectories
                    it.sourceGeneratedDir.set(
                        srcProject.layout.buildDirectory.dir("generated/fakt"),
                    )

                    // Base directory for platform-specific collection
                    // Task will create subdirectories: commonMain/, jvmMain/, etc.
                    it.destinationDir.set(
                        project.layout.buildDirectory.dir("generated/collected-fakes/_placeholder"),
                    )

                    // Add dependency on source project's MAIN compilation tasks only
                    // Avoid test compilations to prevent circular dependencies
                    it.dependsOn(
                        srcProject.tasks.matching { task ->
                            task.name.contains("compile", ignoreCase = true) &&
                                !task.name.contains("test", ignoreCase = true)
                        },
                    )
                }

            // Register ALL *Main source sets (commonMain, jvmMain, iosMain, etc.)
            kotlinExtension.sourceSets
                .matching { sourceSet ->
                    sourceSet.name.endsWith("Main")
                }.configureEach { sourceSet ->
                    val platformDir =
                        task.map {
                            it.destinationDir.asFile
                                .get()
                                .parentFile // up from _placeholder
                                .resolve("${sourceSet.name}/kotlin")
                        }
                    sourceSet.kotlin.srcDir(platformDir)
                    project.logger.info(
                        "Fakt: Registered ${sourceSet.name} for platform-specific collected fakes",
                    )
                }

            project.logger.info(
                "Fakt: Registered collector task '$taskName' with platform detection",
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

            project.logger.info(
                "Fakt: Registered dynamic collector task 'collectFakes' " +
                    "(auto-discovers all generated fakes)",
            )
        }
    }
}
