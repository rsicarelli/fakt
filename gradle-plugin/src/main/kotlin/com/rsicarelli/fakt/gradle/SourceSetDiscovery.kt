// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Discovers and builds complete source set context for a compilation.
 *
 * **Architecture**:
 * ```
 * KotlinCompilation
 *       ↓
 * SourceSetDiscovery.buildContext()
 *       ↓
 * SourceSetContext (serializable)
 *       ↓
 * Compiler Plugin
 * ```
 */
internal object SourceSetDiscovery {
    private const val MAIN_SUFFIX_LENGTH = 4 // Length of "Main" suffix

    /**
     * Maps a main compilation name to its corresponding test source set.
     *
     * This function transforms main source set names to their test equivalents following
     * Kotlin's standard naming conventions. Test compilations are returned unchanged.
     *
     * ## Mapping Rules
     *
     * **Main → Test transformations:**
     * ```
     * "main" → "test"
     * "jvmMain" → "jvmTest"
     * "commonMain" → "commonTest"
     * "iosX64Main" → "iosX64Test"
     * ```
     *
     * **Already test compilations (unchanged):**
     * ```
     * "test" → "test"
     * "jvmTest" → "jvmTest"
     * "commonTest" → "commontest"
     * ```
     *
     * @param compilationName The compilation name to map (e.g., "main", "jvmMain", "test")
     * @return The corresponding test source set name in lowercase
     */
    private fun mapToTestSourceSet(compilationName: String): String {
        // If already a test compilation, return as-is
        if (compilationName.contains("test", ignoreCase = true)) {
            return compilationName.lowercase()
        }

        // Map main compilations to test:
        // "main" → "test"
        // "jvmMain" → "jvmTest"
        // "commonMain" → "commonTest"
        return when {
            compilationName.equals("main", ignoreCase = true) -> "test"
            compilationName.endsWith("Main", ignoreCase = true) -> {
                val prefix = compilationName.dropLast(MAIN_SUFFIX_LENGTH)
                "${prefix}Test"
            }

            else -> "test" // Default fallback
        }
    }

    /**
     * Build complete source set context from a compilation.
     *
     * This is the main entry point for source set discovery. It analyzes a Kotlin compilation
     * and produces a [SourceSetContext] containing all metadata needed by the compiler plugin.
     *
     * ## Algorithm
     *
     * 1. Classify compilation (test vs main)
     * 2. Get default source set from compilation
     * 3. Traverse source set graph to find all parents
     * 4. Extract platform type and target metadata
     * 5. Build hierarchy map for compiler plugin
     * 6. Generate output directory path
     * 7. Package into [SourceSetContext]
     *
     * ## Examples
     *
     * **KMP project with commonMain:**
     * ```kotlin
     * // Input: jvmMain compilation
     * val context = buildContext(jvmMainCompilation, "/path/to/build")
     *
     * // Output:
     * SourceSetContext(
     *   compilationName = "main",
     *   targetName = "jvm",
     *   platformType = "jvm",
     *   isTest = false,
     *   defaultSourceSet = SourceSetInfo("jvmMain", parents=["commonMain"]),
     *   allSourceSets = [jvmMain, commonMain],
     *   outputDirectory = "/path/to/build/generated/fakt/commonTest/kotlin"
     *   // ^ Note: commonTest because commonMain detected in hierarchy
     * )
     * ```
     *
     * **Single-platform JVM project:**
     * ```kotlin
     * // Input: test compilation
     * val context = buildContext(testCompilation, "/path/to/build")
     *
     * // Output:
     * SourceSetContext(
     *   compilationName = "test",
     *   targetName = "jvm",
     *   platformType = "jvm",
     *   isTest = true,
     *   defaultSourceSet = SourceSetInfo("test", parents=[]),
     *   allSourceSets = [test],
     *   outputDirectory = "/path/to/build/generated/fakt/test/kotlin"
     * )
     * ```
     *
     * @param compilation The Kotlin compilation to analyze (e.g., jvmMain, commonTest)
     * @param buildDir The project's build directory absolute path (e.g., "/path/to/build")
     * @return Complete [SourceSetContext] ready for serialization to compiler plugin
     * @see SourceSetContext
     */
    fun buildContext(
        compilation: KotlinCompilation<*>,
        buildDir: String,
    ): SourceSetContext {
        // 1. Classify compilation (test vs main)
        val isTest = compilation.isTestCompilation

        // 2. Get default source set
        val defaultSourceSet = compilation.defaultSourceSet

        // 3. Traverse source set graph to get all parents
        val allSourceSets = defaultSourceSet.getAllParentSourceSets()

        // 4. Build source set info list with hierarchy
        val sourceSetInfos =
            allSourceSets.map { sourceSet ->
                val parentNames =
                    sourceSet.dependsOn
                        .map { it.name }
                        .sorted() // Deterministic ordering

                SourceSetInfo(
                    name = sourceSet.name,
                    parents = parentNames,
                )
            }

        // 5. Find the default source set info
        val defaultSourceSetInfo = sourceSetInfos.first { it.name == defaultSourceSet.name }

        // 6. Extract target metadata
        val platformType = compilation.target.platformType.name
        // For JVM-only projects (kotlin-jvm plugin), targetName may be blank
        // Use platformType as fallback (e.g., "jvm" for JVM-only projects)
        val targetName = compilation.target.targetName.ifBlank { platformType.lowercase() }

        // 7. Generate output directory
        // Always output to test source sets since fakes are only used in tests
        //
        // OUTPUT STRATEGY: For KMP projects, ALWAYS output to commonTest if commonMain
        // exists in the compilation's source set hierarchy. This centralizes all fakes
        // in one location and relies on KMP's KLIB dependency propagation to make them
        // visible across all platform tests (jvmTest, iosX64Test, etc.).
        //
        // CRITICAL: Do NOT register commonTest source directory to platform test source
        // sets in SourceSetConfigurator - this causes "can be a part of only one module"
        // errors. KMP propagates COMPILED code (KLIBs), not source directories.
        //
        // Example:
        //   - Interfaces in commonMain → Fakes in commonTest → Compiled to KLIB
        //   - jvmTest depends on commonTest → Can import fakes (via KLIB)
        //   - iosX64Test depends on commonTest → Can import fakes (via KLIB)
        val hasCommonMain = allSourceSets.any { it.name == "commonMain" }
        val testSourceSet =
            if (hasCommonMain) {
                "commonTest"
            } else {
                // Non-KMP or platform-specific: use platform test source set
                mapToTestSourceSet(defaultSourceSet.name)
            }
        val outputDirectory = "$buildDir/generated/fakt/$testSourceSet/kotlin"

        // 8. Compute KMP cross-compilation cache paths
        // Producer mode (metadata compilation): Writes cache for platform compilations
        // Consumer mode (platform compilations): Reads cache from metadata compilation
        val cacheFilePath = "$buildDir/generated/fakt/cache/fir-metadata.json"

        val (metadataOutputPath, metadataCachePath) =
            computeCachePaths(
                platformType = platformType,
                hasCommonMain = hasCommonMain,
                cacheFilePath = cacheFilePath,
            )

        // 9. Package into context
        return SourceSetContext(
            compilationName = compilation.name,
            targetName = targetName,
            platformType = platformType,
            isTest = isTest,
            defaultSourceSet = defaultSourceSetInfo,
            allSourceSets = sourceSetInfos,
            outputDirectory = outputDirectory,
            metadataOutputPath = metadataOutputPath,
            metadataCachePath = metadataCachePath,
        )
    }

    /**
     * Compute cache paths for KMP cross-compilation optimization.
     *
     * In KMP projects, the metadata compilation (common platform) performs FIR analysis
     * and writes a cache file. Platform compilations then read this cache to skip
     * redundant FIR analysis.
     *
     * **Producer Mode** (metadata compilation):
     * - `platformType` == "common"
     * - Sets `metadataOutputPath` to write cache
     * - Returns `metadataCachePath` = null
     *
     * **Consumer Mode** (platform compilations):
     * - `platformType` != "common" AND hasCommonMain == true
     * - Sets `metadataCachePath` to read cache
     * - Returns `metadataOutputPath` = null
     *
     * **Non-KMP** (single-platform projects):
     * - Both paths are null (no cross-compilation caching needed)
     *
     * @param platformType The platform type (e.g., "common", "jvm", "native")
     * @param hasCommonMain Whether the project has a commonMain source set
     * @param cacheFilePath The absolute path to the cache file
     * @return Pair of (metadataOutputPath, metadataCachePath)
     */
    private fun computeCachePaths(
        platformType: String,
        hasCommonMain: Boolean,
        cacheFilePath: String,
    ): Pair<String?, String?> {
        // Non-KMP projects: no cross-compilation caching
        if (!hasCommonMain) {
            return null to null
        }

        // KMP project: determine producer vs consumer mode
        return if (platformType.equals("common", ignoreCase = true)) {
            // Producer mode: metadata compilation writes cache
            cacheFilePath to null
        } else {
            // Consumer mode: platform compilation reads cache
            null to cacheFilePath
        }
    }
}

/**
 * Classifies Kotlin compilations as test or production code.
 *
 * **Purpose**: Determine if a compilation should generate fakes in test or main source sets.
 *
 * **Heuristics** (evaluated in order):
 * 1. **Standard test compilation name**: `compilation.name == "test"`
 * 2. **Convention**: `compilation.name.endsWith("Test", ignoreCase = true)`
 * 3. **Associated with main**: Compilation is associated with main compilation (test suite pattern)
 *
 * **Examples**:
 * - `test` → true (standard)
 * - `main` → false (standard)
 * - `integrationTest` → true (convention)
 * - `e2eTest` → true (convention)
 * - `debug` → false (Android variant, not test)
 * - `debugTest` → true (Android test variant)
 * - Custom suite associated with main → true (association pattern)
 *
 * @param this@isTestCompilation The compilation to classify
 * @return true if this is a test compilation, false if it's production code
 */
public val KotlinCompilation<*>.isTestCompilation: Boolean
    get() {
        // Heuristic 1: Standard test compilation name
        if (this.name == KotlinCompilation.TEST_COMPILATION_NAME) {
            return true
        }

        // Heuristic 2: Convention - name ends with "Test" (case-insensitive)
        // Heuristic 3: Associated with main compilation (test suite pattern)
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        val associatedCompilations = this.allAssociatedCompilations
        return this.name.endsWith("Test", ignoreCase = true) ||
            associatedCompilations.any { it.name == KotlinCompilation.MAIN_COMPILATION_NAME }
    }

/**
 * Traverse the dependsOn graph upwards using BFS to find all parent source sets.
 * Includes the starting source set in the result.
 *
 * **Example 1 - Simple hierarchy**:
 * ```
 * jvmMain.dependsOn(commonMain)
 * Result: Set(jvmMain, commonMain)
 * ```
 *
 * **Example 2 - Deep hierarchy**:
 * ```
 * iosX64Main → iosMain → appleMain → nativeMain → commonMain
 * Result: Set(iosX64Main, iosMain, appleMain, nativeMain, commonMain)
 * ```
 *
 * **Example 3 - Diamond dependency**:
 * ```
 *        commonMain
 *        /        \
 *   nativeMain   appleMain
 *        \        /
 *         iosMain
 * Result: Set(iosMain, nativeMain, appleMain, commonMain)
 * ```
 * @receiver [KotlinSourceSet]
 * @return Set of all source sets in the hierarchy (including starting set)
 */
public fun KotlinSourceSet.getAllParentSourceSets(): Set<KotlinSourceSet> {
    val allParents = mutableSetOf<KotlinSourceSet>()
    val queue = ArrayDeque<KotlinSourceSet>()

    // Start with the initial source set
    queue.add(this)
    allParents.add(this)

    // BFS traversal
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()

        // For each direct parent, add it to the set and queue if not seen before
        current.dependsOn.forEach { parent ->
            if (allParents.add(parent)) {
                queue.add(parent)
            }
        }
    }

    return allParents
}
