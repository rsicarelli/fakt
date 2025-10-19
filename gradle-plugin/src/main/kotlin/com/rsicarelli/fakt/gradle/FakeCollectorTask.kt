// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
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
 * - `module.jvm.*` packages → `jvmMain/kotlin/`
 * - `module.ios.*` packages → `iosMain/kotlin/`
 * - `module.common.*` packages → `commonMain/kotlin/`
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
 *
 * @see ExperimentalFaktMultiModule
 */
@ExperimentalFaktMultiModule
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

    /**
     * Available source set names from the project's KMP configuration.
     * Used for dynamic platform detection instead of hardcoded platform list.
     * Empty set means fallback to legacy hardcoded behavior.
     *
     * Example: ["commonMain", "jvmMain", "iosMain", "tvosMain", "watchosMain", ...]
     */
    @get:Input
    @get:Optional
    abstract val availableSourceSets: SetProperty<String>

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

            // Use platform detection for this source set (with available source sets if configured)
            val sourceSetNames = availableSourceSets.getOrElse(emptySet())
            val result =
                collectWithPlatformDetection(
                    sourceDir = kotlinDir,
                    destinationBaseDir = destinationBaseDir,
                    availableSourceSets = sourceSetNames,
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
         * 2. Dynamically match package segments against real source sets from project
         * 3. Prioritize shortest match (closest to package segment name)
         * 4. Falls back to commonMain if no match found
         *
         * **Dynamic Matching**:
         * - Extracts package segments (e.g., "api.tvos.services" → ["api", "tvos", "services"])
         * - Finds source sets that start with each segment (case-insensitive)
         * - Prioritizes hierarchical source sets over architecture-specific ones
         *   (e.g., iosMain over iosArm64Main for package "api.ios.services")
         * - Falls back to commonMain if no match found
         *
         * @param fileContent The content of the fake file
         * @param availableSourceSets Set of source set names available in the project
         *   (from KotlinMultiplatformExtension.sourceSets.names)
         * @return The source set name (e.g., "jvmMain", "commonMain", "iosMain")
         */
        fun determinePlatformSourceSet(
            fileContent: String,
            availableSourceSets: Set<String> = emptySet(),
        ): String {
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

            // Dynamic matching using real source sets from project
            return matchPackageToSourceSet(packageSegments, availableSourceSets)
        }

        /**
         * Match package segments to available source sets dynamically.
         * Prioritizes matches that are closest to the package segment.
         *
         * Strategy:
         * 1. Find all source sets that match package segments (case-insensitive prefix match)
         * 2. Prioritize shortest match (closest to package segment name)
         * 3. This ensures hierarchical source sets are preferred over architecture-specific ones
         *
         * Example 1 - Hierarchical match:
         * - Package: "api.ios.services" (segment: "ios")
         * - Available: ["iosMain", "iosArm64Main", "iosX64Main"]
         * - Matches: ["iosMain" (7 chars), "iosArm64Main" (13 chars), "iosX64Main" (10 chars)]
         * - Result: "iosMain" (shortest/closest match)
         *
         * Example 2 - Architecture-specific match:
         * - Package: "api.iosArm64.services" (segment: "iosArm64")
         * - Available: ["iosMain", "iosArm64Main"]
         * - Matches: ["iosArm64Main" (13 chars - exact match minus "Main")]
         * - Result: "iosArm64Main" (exact architecture match)
         *
         * @param packageSegments List of package segments (e.g., ["api", "ios", "services"])
         * @param availableSourceSets Set of source set names from project
         * @return Matched source set name or "commonMain" as fallback
         */
        private fun matchPackageToSourceSet(
            packageSegments: List<String>,
            availableSourceSets: Set<String>,
        ): String {
            // Find all matching source sets for any package segment
            // Store as pairs of (sourceSet, matchingSegment) to know which segment matched
            val matchedSourceSets =
                packageSegments
                    .flatMap { segment ->
                        availableSourceSets
                            .filter { sourceSet ->
                                // Match if source set name starts with segment (case-insensitive)
                                // Examples:
                                // - "ios" matches "iosMain", "iosArm64Main", "iosTest"
                                // - "tvos" matches "tvosMain", "tvosArm64Main"
                                // - "wasmJs" matches "wasmJsMain"
                                sourceSet.startsWith(segment, ignoreCase = true) && sourceSet.endsWith("Main")
                            }.map { sourceSet -> sourceSet to segment } // Keep track of which segment matched
                    }.distinct()

            // If no matches, fallback to commonMain
            if (matchedSourceSets.isEmpty()) {
                return "commonMain"
            }

            // Prioritize source set with shortest name (closest match to package segment)
            // This prioritizes hierarchical source sets over architecture-specific ones:
            // - "ios" → "iosMain" (7 chars) preferred over "iosArm64Main" (13 chars)
            // - "iosArm64" → "iosArm64Main" (13 chars) is the closest match
            // - "tvos" → "tvosMain" (8 chars) preferred over "tvosSimulatorArm64Main" (23 chars)
            return matchedSourceSets.minByOrNull { (sourceSet, _) -> sourceSet.length }?.first ?: "commonMain"
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
         * @param availableSourceSets Set of available source sets for dynamic detection (optional)
         * @return Collection result with statistics
         */
        fun collectWithPlatformDetection(
            sourceDir: File,
            destinationBaseDir: File,
            availableSourceSets: Set<String> = emptySet(),
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
                    val platform = determinePlatformSourceSet(fileContent, availableSourceSets)

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
         * @see ExperimentalFaktMultiModule
         */
        @ExperimentalFaktMultiModule
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

            // Extract available source set names for dynamic platform detection
            val availableSourceSetNames = kotlinExtension.sourceSets.names

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

                    // Configure available source sets for dynamic platform detection
                    // This enables support for ALL KMP targets without hardcoding
                    it.availableSourceSets.set(availableSourceSetNames)

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
         *
         * @see ExperimentalFaktMultiModule
         */
        @ExperimentalFaktMultiModule
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
