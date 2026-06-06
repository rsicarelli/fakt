// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.TimeFormatter
import com.rsicarelli.fakt.gradle.FakeCollectorTask.Companion.registerForKmpProject
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile

/**
 * Task to collect generated fakes from a source project with platform-aware placement.
 *
 * This task intelligently analyzes package structure to determine target platform and places fakes
 * in appropriate source sets:
 * - `module.jvm.*` packages → `jvmMain/kotlin/`
 * - `module.ios.*` packages → `iosMain/kotlin/`
 * - `module.common.*` packages → `commonMain/kotlin/`
 *
 * This solves the cross-platform compilation problem where JVM-only interfaces would fail to
 * compile in commonMain.
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
@CacheableTask
@ExperimentalFaktMultiModule
public abstract class FakeCollectorTask : DefaultTask() {

    /**
     * The path to the source project that generates fakes. Configuration cache compatible (stores
     * path, not Project object).
     */
    @get:Input @get:Optional public abstract val sourceProjectPath: Property<String>

    /**
     * The directory where the source project generates fakes. Typically: build/generated/fakt/
     * Optional because not all source sets may have generated fakes.
     *
     * Used only by the legacy in-process compiler-plugin path, which writes fakes as a side effect
     * of `compileKotlin*` and therefore cannot be declared as a Gradle task input. Under the
     * cache-correct path (`useExperimentalGenerateTask=true`), [sourceFakeRoots] supersedes this
     * property — see the doc on that field.
     */
    @get:Internal // Not using @InputDirectory to allow missing directories
    public abstract val sourceGeneratedDir: DirectoryProperty

    /**
     * Aggregated `generatedKotlinDir` outputs of every [FaktGenerateTask] registered on the source
     * project. Wired through `ConfigurableFileCollection.from(Provider)` so Gradle carries the
     * `builtBy` chain automatically — no string-matched `dependsOn(matching { name == "compile…"
     * })`, which silently breaks the dependency chain when task names change.
     *
     * Declared `@InputFiles` rather than `@Internal` so the collector's own cache key fingerprints
     * the source `.kt` payload — the cache-correct path is end-to-end, not just at the producer.
     *
     * Empty when the source project still uses the legacy in-process compiler-plugin path, in which
     * case the action falls back to polling [sourceGeneratedDir].
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    public abstract val sourceFakeRoots: ConfigurableFileCollection

    /**
     * The destination directory where collected fakes will be placed. Typically:
     * src/commonMain/kotlin/ or build/generated/collected-fakes/
     */
    @get:OutputDirectory public abstract val destinationDir: DirectoryProperty

    /**
     * Available source set names from the project's KMP configuration. Used for dynamic platform
     * detection instead of hardcoded platform list. Empty set means fallback to legacy hardcoded
     * behavior.
     *
     * Example: ["commonMain", "jvmMain", "iosMain", "tvosMain", "watchosMain", ...]
     */
    @get:Input @get:Optional public abstract val availableSourceSets: SetProperty<String>

    /**
     * Log level for controlling output verbosity. Respects the same logLevel configuration as the
     * compiler plugin.
     * - QUIET: No output
     * - INFO: Summary only (default)
     * - DEBUG: Summary + detailed per-source-set info
     * - TRACE: Summary + details + registration info
     */
    @get:Input public abstract val logLevel: Property<LogLevel>

    init {
        group = "fakt"
        description = "Collects generated fakes from source project"

        // Caching is only correct when sources arrive through `sourceFakeRoots`. Under the legacy
        // in-process path the action polls `sourceGeneratedDir` at execution time — not declared as
        // an input, so any "cache hit" would be a stale-output bug. This gate goes away together
        // with the legacy path, making the task unconditionally cacheable.
        @Suppress("LeakingThis")
        outputs.cacheIf("sourceFakeRoots wired (cache-correct path)") { !sourceFakeRoots.isEmpty }
    }

    @TaskAction
    public fun collectFakes() {
        val startTime = System.nanoTime()
        val faktLogger = GradleFaktLogger(logger, logLevel.get())

        val taskProviderRoots = sourceFakeRoots.files.filter { it.isDirectory }
        val sourceSetNames = availableSourceSets.getOrElse(mutableSetOf())
        // `destinationDir` is the routing root — platform subdirs (`jvmMain/kotlin/...`) are
        // written directly under it so Gradle's build cache fingerprints every routed file as part
        // of this task's @OutputDirectory. Writing outside this root silently disables cache
        // restore for the collector.
        val destinationBaseDir = destinationDir.asFile.get()
        val platformStats = mutableMapOf<String, Int>()
        val totalCollected =
            if (taskProviderRoots.isNotEmpty()) {
                collectFromTaskProviderRoots(
                    roots = taskProviderRoots,
                    destinationBaseDir = destinationBaseDir,
                    availableSourceSets = sourceSetNames,
                    faktLogger = faktLogger,
                    platformStats = platformStats,
                )
            } else {
                collectFromLegacySourceDir(
                    destinationBaseDir = destinationBaseDir,
                    availableSourceSets = sourceSetNames,
                    faktLogger = faktLogger,
                    platformStats = platformStats,
                )
            } ?: return

        val totalDuration = System.nanoTime() - startTime
        val srcProjectName = sourceProjectPath.orNull?.substringAfterLast(":") ?: "unknown"

        faktLogger.info(
            "✅ $totalCollected fake(s) collected from $srcProjectName | " +
                TimeFormatter.format(totalDuration)
        )
        platformStats.forEach { (platform, count) ->
            faktLogger.info("  ├─ $platform: $count file(s)")
        }
    }

    /**
     * Cache-correct path. Each entry of [roots] is a [FaktGenerateTask.generatedKotlinDir] — a
     * package-structured Kotlin source root. Files are routed by package-derived platform detection
     * exactly like the legacy path; the only difference is that we never poll a source-project
     * directory at execution time, so Gradle's build cache fingerprints the actual inputs.
     */
    private fun collectFromTaskProviderRoots(
        roots: List<File>,
        destinationBaseDir: File,
        availableSourceSets: Set<String>,
        faktLogger: GradleFaktLogger,
        platformStats: MutableMap<String, Int>,
    ): Int {
        var totalCollected = 0
        roots.forEach { root ->
            val rootStartTime = System.nanoTime()
            val result =
                collectWithPlatformDetection(
                    sourceDir = root,
                    destinationBaseDir = destinationBaseDir,
                    availableSourceSets = availableSourceSets,
                    faktLogger = faktLogger,
                )
            totalCollected += result.collectedCount
            result.platformDistribution.forEach { (platform, count) ->
                platformStats[platform] = (platformStats[platform] ?: 0) + count
            }
            faktLogger.debug(
                "Collected ${result.collectedCount} fake(s) from ${root.name} " +
                    "(${TimeFormatter.format(System.nanoTime() - rootStartTime)})"
            )
        }
        return totalCollected
    }

    /**
     * Legacy in-process path. Walks the source project's `build/generated/fakt/` directory at
     * execution time. Returns `null` when nothing is found (caller short-circuits the summary log).
     */
    private fun collectFromLegacySourceDir(
        destinationBaseDir: File,
        availableSourceSets: Set<String>,
        faktLogger: GradleFaktLogger,
        platformStats: MutableMap<String, Int>,
    ): Int? {
        val faktRootDir = sourceGeneratedDir.asFile.get()
        val sourceSetDirs =
            faktRootDir.takeIf { it.exists() }?.listFiles()?.filter { it.isDirectory }.orEmpty()
        if (sourceSetDirs.isEmpty()) {
            warnLegacyEmpty(faktRootDir, faktLogger)
            return null
        }

        var totalCollected = 0
        sourceSetDirs.forEach { sourceSetDir ->
            val kotlinDir = sourceSetDir.resolve("kotlin")
            if (!kotlinDir.exists() || !kotlinDir.isDirectory) {
                faktLogger.debug("Skipping ${sourceSetDir.name} (no kotlin directory)")
                return@forEach
            }
            val sourceSetStartTime = System.nanoTime()
            val result =
                collectWithPlatformDetection(
                    sourceDir = kotlinDir,
                    destinationBaseDir = destinationBaseDir,
                    availableSourceSets = availableSourceSets,
                    faktLogger = faktLogger,
                )
            totalCollected += result.collectedCount
            result.platformDistribution.forEach { (platform, count) ->
                platformStats[platform] = (platformStats[platform] ?: 0) + count
            }
            faktLogger.debug(
                "Collected ${result.collectedCount} fake(s) from ${sourceSetDir.name} " +
                    "(${TimeFormatter.format(System.nanoTime() - sourceSetStartTime)})"
            )
        }
        return totalCollected
    }

    private fun warnLegacyEmpty(faktRootDir: File, faktLogger: GradleFaktLogger) {
        if (!faktRootDir.exists()) {
            val srcProjectName = sourceProjectPath.orNull?.substringAfterLast(":") ?: "unknown"
            faktLogger.warn(
                "No fakes found in source module '$srcProjectName'. " +
                    "Verify that source module has @Fake annotated interfaces, " +
                    "or remove this collector module if not needed."
            )
        } else {
            faktLogger.warn("No generated fakes found in $faktRootDir")
        }
    }

    public companion object {
        private const val PACKAGE_SCAN_LINES = 10 // Number of lines to scan for package declaration

        /**
         * Determines the appropriate platform source set based on file content. Analyzes the
         * package declaration to detect platform-specific markers.
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
         * - Prioritizes hierarchical source sets over architecture-specific ones (e.g., iosMain
         *   over iosArm64Main for package "api.ios.services")
         * - Falls back to commonMain if no match found
         *
         * @param fileContent The content of the fake file
         * @param availableSourceSets Set of source set names available in the project (from
         *   KotlinMultiplatformExtension.sourceSets.names)
         * @return The source set name (e.g., "jvmMain", "commonMain", "iosMain")
         */
        public fun determinePlatformSourceSet(
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
                    ?.trim() ?: return "commonMain" // No package → commonMain

            // Split package into segments for analysis
            val packageSegments = packageDeclaration.split(".")

            // Dynamic matching using real source sets from project
            return matchPackageToSourceSet(packageSegments, availableSourceSets)
        }

        /**
         * Match package segments to available source sets dynamically. Prioritizes matches that are
         * closest to the package segment.
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
                                // Collector modules expose fakes in MAIN source sets (as libraries)
                                // Examples:
                                // - "ios" matches "iosMain", "iosArm64Main"
                                // - "tvos" matches "tvosMain", "tvosArm64Main"
                                // - "wasmJs" matches "wasmJsMain"
                                sourceSet.startsWith(segment, ignoreCase = true) &&
                                    sourceSet.endsWith("Main")
                            }
                            .map { sourceSet ->
                                sourceSet to segment
                            } // Keep track of which segment matched
                    }
                    .distinct()

            // If no matches, fallback to commonMain (collector modules publish as libraries)
            if (matchedSourceSets.isEmpty()) {
                return "commonMain"
            }

            // Prioritize source set with shortest name (closest match to package segment)
            // This prioritizes hierarchical source sets over architecture-specific ones:
            // - "ios" → "iosMain" (7 chars) preferred over "iosArm64Main" (13 chars)
            // - "iosArm64" → "iosArm64Main" (13 chars) is the closest match
            // - "tvos" → "tvosMain" (8 chars) preferred over "tvosSimulatorArm64Main" (23 chars)
            return matchedSourceSets.minByOrNull { (sourceSet, _) -> sourceSet.length }?.first
                ?: "commonMain"
        }

        /**
         * Result of collecting fakes with platform detection.
         *
         * @property collectedCount Total number of files collected
         * @property platformDistribution Map of platform → file count
         */
        public data class CollectionResult(
            val collectedCount: Int,
            val platformDistribution: Map<String, Int>,
        )

        /**
         * Collects fakes from source directory with platform-specific placement.
         *
         * Scans the source directory for Kotlin files, detects their target platform based on
         * package structure, and places them in the appropriate platform source set directory.
         *
         * @param sourceDir Directory containing generated fakes
         * @param destinationBaseDir Base directory for collected fakes
         * @param availableSourceSets Set of available source sets for dynamic detection (optional)
         * @param faktLogger Logger for trace-level file processing details (optional)
         * @return Collection result with statistics
         */
        public fun collectWithPlatformDetection(
            sourceDir: File,
            destinationBaseDir: File,
            availableSourceSets: Set<String> = emptySet(),
            faktLogger: GradleFaktLogger? = null,
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
                        destinationBaseDir.resolve("$platform/kotlin").resolve(relativePath)

                    // Create parent directories and copy file
                    destFile.parentFile.mkdirs()
                    sourceFile.copyTo(destFile, overwrite = true)

                    faktLogger?.debug(
                        "${sourceFile.name} → $platform (${fileContent.length} bytes)"
                    )

                    // Update statistics
                    collectedCount++
                    platformDistribution[platform] = (platformDistribution[platform] ?: 0) + 1
                }

            return CollectionResult(collectedCount, platformDistribution)
        }

        /**
         * Register collector task for a KMP project with platform-aware collection.
         *
         * This creates a single task that auto-discovers all generated fakes and intelligently
         * places them based on package structure:
         * - api.jvm.* → jvmMain/kotlin/
         * - api.ios.* → iosMain/kotlin/
         * - api.shared.* or api.common.* → commonMain/kotlin/
         *
         * Registers ALL *Main source sets (commonMain, jvmMain, iosMain, etc.) so each platform can
         * access its appropriate fakes.
         *
         * @param project The target project (collector module)
         * @param extension The Fakt plugin extension
         * @see ExperimentalFaktMultiModule
         */
        @ExperimentalFaktMultiModule
        public fun registerForKmpProject(project: Project, extension: FaktPluginExtension) {
            val srcProject = extension.collectFrom.orNull ?: return

            val kotlinExtension =
                project.extensions.findByType(KotlinMultiplatformExtension::class.java)
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

                    // Cache-correct path. `from(Provider)` carries `builtBy` automatically so the
                    // collector waits for every `FaktGenerateTask` on the source project — no
                    // string-matched `dependsOn(matching { ... })`. Empty under the legacy
                    // in-process path.
                    it.sourceFakeRoots.from(
                        srcProject.tasks.withType(FaktGenerateTask::class.java).map { generateTask
                            ->
                            generateTask.generatedKotlinDir
                        }
                    )

                    // Legacy fallback: source project still on the in-process compiler-plugin path.
                    // Polled at execution time; not a declared input.
                    it.sourceGeneratedDir.set(
                        srcProject.layout.buildDirectory.dir("generated/fakt")
                    )

                    // Routing root for platform-specific collection. The action creates
                    // subdirectories `commonMain/kotlin`, `jvmMain/kotlin`, … directly under this
                    // dir, so the build cache fingerprints every routed file.
                    it.destinationDir.set(
                        project.layout.buildDirectory.dir("generated/collected-fakes")
                    )

                    // Configure available source sets for dynamic platform detection
                    // This enables support for ALL KMP targets without hardcoding
                    it.availableSourceSets.set(availableSourceSetNames)

                    // Wire logLevel from extension for consistent telemetry
                    it.logLevel.set(extension.logLevel)

                    // Legacy ordering fallback for the in-process path. When the source uses
                    // `FaktGenerateTask` instead, `sourceFakeRoots` above already pulls the
                    // implicit dependency through `builtBy`, so this matcher is redundant but
                    // harmless.
                    it.dependsOn(
                        srcProject.tasks.matching { task ->
                            task.name.contains("compile", ignoreCase = true) &&
                                !task.name.contains("test", ignoreCase = true)
                        }
                    )
                }

            // Register ALL *Main source sets (commonMain, jvmMain, iosMain, etc.)
            // CRITICAL: Collector modules expose fakes in MAIN source sets so consumer modules
            // can depend on them. While fakes are test utilities, collector modules act as
            // LIBRARIES that publish fakes for other modules to use.
            //
            // Architecture:
            //   Source module → generates fakes in TEST (for own tests)
            //   Collector module → collects fakes and exposes in MAIN (as library)
            //   Consumer module → depends on collector MAIN (uses fakes in its tests)
            kotlinExtension.sourceSets
                .matching { sourceSet -> sourceSet.name.endsWith("Main") }
                .configureEach { sourceSet ->
                    val platformDir =
                        task.map {
                            it.destinationDir.asFile.get().resolve("${sourceSet.name}/kotlin")
                        }
                    sourceSet.kotlin.srcDir(platformDir)

                    project.logger.info(
                        "Fakt: Registered collected fakes to ${sourceSet.name}: $platformDir"
                    )

                    // Wire task dependencies: ensure MAIN compilation tasks depend on collectFakes
                    // This guarantees fakes are collected before compilation
                    project.tasks
                        .matching { compileTask ->
                            // Type-based: only Kotlin compilation tasks
                            (compileTask is KotlinCompile ||
                                compileTask is Kotlin2JsCompile ||
                                compileTask is KotlinNativeCompile) &&
                                // Name-based: match source set name
                                compileTask.name.contains(sourceSet.name, ignoreCase = true) &&
                                // CRITICAL: Only MAIN compilations (collector modules publish as
                                // libraries)
                                !compileTask.name.contains("test", ignoreCase = true)
                        }
                        .configureEach { compileTask -> compileTask.dependsOn(task) }
                }
        }

        /**
         * Register a single collector task for single-platform projects.
         *
         * Uses the same auto-discovery approach as KMP projects for consistency.
         *
         * ## Supported Platforms
         * - **JVM**: `org.jetbrains.kotlin.jvm` plugin
         * - **Android Library**: `com.android.library` plugin
         * - **Android Application**: `com.android.application` plugin
         * - **Kotlin/JS**: `org.jetbrains.kotlin.js` plugin
         * - **KMP Projects**: Use [registerForKmpProject] instead
         *
         * ## Task Dependency Strategy
         *
         * Uses type-based task matching for robustness:
         * - JVM/Android: [KotlinCompile] tasks
         * - JavaScript: [Kotlin2JsCompile] tasks
         * - Native: Handled via KMP code path
         *
         * This ensures only Kotlin compilation tasks depend on `collectFakes`, avoiding false
         * positives from Java/Groovy compilation tasks.
         *
         * @see ExperimentalFaktMultiModule
         */
        @ExperimentalFaktMultiModule
        private fun registerSingleCollectorTask(project: Project, extension: FaktPluginExtension) {
            val srcProject = extension.collectFrom.orNull ?: return

            val task =
                project.tasks.register("collectFakes", FakeCollectorTask::class.java) {
                    it.sourceProjectPath.set(srcProject.path)

                    // Cache-correct path; see KDoc on the KMP variant for the rationale.
                    it.sourceFakeRoots.from(
                        srcProject.tasks.withType(FaktGenerateTask::class.java).map { generateTask
                            ->
                            generateTask.generatedKotlinDir
                        }
                    )

                    // Legacy fallback (in-process compiler plugin).
                    it.sourceGeneratedDir.set(
                        srcProject.layout.buildDirectory.dir("generated/fakt")
                    )

                    // Routing root; mirrors the KMP variant — see KDoc on the KMP setup.
                    it.destinationDir.set(
                        project.layout.buildDirectory.dir("generated/collected-fakes")
                    )

                    // Wire logLevel from extension for consistent telemetry
                    it.logLevel.set(extension.logLevel)

                    // Legacy ordering fallback; redundant once `sourceFakeRoots` is wired.
                    it.dependsOn(
                        srcProject.tasks.matching { task ->
                            task.name.contains("compile", ignoreCase = true) &&
                                !task.name.contains("test", ignoreCase = true)
                        }
                    )
                }

            // Single-platform projects never supply `availableSourceSets`, so the action routes
            // every fake to the `commonMain` fallback subdir. Source set wiring follows that
            // convention.
            val collectedDir =
                task.map { it.destinationDir.asFile.get().resolve("commonMain/kotlin") }

            // === JVM Projects ===
            project.plugins.withId("org.jetbrains.kotlin.jvm") {
                project.extensions.findByType(KotlinJvmProjectExtension::class.java)?.apply {
                    sourceSets.getByName("main").kotlin.srcDir(collectedDir)

                    // Type-based task dependencies for JVM compilation
                    project.tasks.withType(KotlinCompile::class.java).configureEach {
                        if (!it.name.contains("test", ignoreCase = true)) {
                            it.dependsOn(task)
                        }
                    }
                }
            }

            // === Android Library Projects ===
            project.plugins.withId("com.android.library") {
                // Android uses Kotlin Android plugin internally (KotlinCompile tasks)
                project.tasks.withType(KotlinCompile::class.java).configureEach {
                    if (!it.name.contains("test", ignoreCase = true)) {
                        it.dependsOn(task)
                    }
                }
            }

            // === Android Application Projects ===
            project.plugins.withId("com.android.application") {
                // Android apps also use KotlinCompile tasks
                project.tasks.withType(KotlinCompile::class.java).configureEach {
                    if (!it.name.contains("test", ignoreCase = true)) {
                        it.dependsOn(task)
                    }
                }
            }

            // === Kotlin/JS Projects ===
            project.plugins.withId("org.jetbrains.kotlin.js") {
                // Use Kotlin2JsCompile for JavaScript compilation tasks
                project.tasks.withType(Kotlin2JsCompile::class.java).configureEach {
                    if (!it.name.contains("test", ignoreCase = true)) {
                        it.dependsOn(task)
                    }
                }
            }
        }
    }
}
