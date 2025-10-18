// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.output

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File

/**
 * Handles source set mapping and output directory resolution for Kotlin Multiplatform projects.
 * Provides intelligent fallback strategies for different KMP target configurations.
 *
 * **Modernization (v1.1.0)**:
 * - Added SourceSetResolver for data-driven source set resolution
 * - Maintains backward compatibility with legacy fallback when resolver is null
 *
 * @param outputDir Gradle-provided output directory (if available)
 * @param messageCollector Compiler message collector for logging
 * @param sourceSetResolver Modern source set resolver (null for legacy fallback)
 * @since 1.0.0
 */
internal class SourceSetMapper(
    private val outputDir: String?,
    private val messageCollector: MessageCollector?,
    private val sourceSetResolver: com.rsicarelli.fakt.compiler.SourceSetResolver? = null,
) {
    /**
     * Get generated sources directory with source set-based routing.
     *
     * **Strategy**:
     * 1. Use provided source set name (extracted from IrFile path) as source of truth
     * 2. Map source set to test source set (jvmMain → jvmTest)
     * 3. Fall back to Gradle-provided output directory if no source set provided
     * 4. Ultimate fallback: intelligent source set mapping from module name
     *
     * **Example**:
     * - Source set: "jvmMain" → output: "build/generated/fakt/jvmTest/kotlin"
     * - Source set: "commonMain" (in jvmMain compilation) → output: "build/generated/fakt/commonTest/kotlin"
     * - Source set: null → fall back to Gradle-provided or module-based mapping
     *
     * This solves the cross-platform compilation problem where a single compilation
     * (e.g., jvmMain) contains files from multiple source sets (jvmMain + commonMain via dependsOn).
     *
     * @param moduleFragment The IR module fragment to analyze
     * @param sourceSetName The source set name extracted from IrFile path (optional)
     * @return File pointing to the appropriate generated sources directory
     */
    fun getGeneratedSourcesDir(
        moduleFragment: IrModuleFragment,
        sourceSetName: String? = null,
    ): File {
        val moduleName = moduleFragment.name.asString().lowercase()

        // If source set provided, use it as source of truth
        if (sourceSetName != null) {
            return resolveOutputDirectoryForSourceSet(sourceSetName)
        }

        // Fall back to Gradle-provided directory (respects SourceSetContext from plugin)
        return tryGradleProvidedOutputDir()
            ?: tryIntelligentSourceSetMapping(moduleName)
    }

    /**
     * Resolves output directory for a specific source set.
     *
     * **Strategy**:
     * 1. Map source set to test source set (jvmMain → jvmTest)
     * 2. Build output path: build/generated/fakt/{testSourceSet}/kotlin
     *
     * **Important**: When source set is explicitly provided (via IrFile extraction),
     * we bypass the Gradle-provided directory to avoid double nesting.
     *
     * @param sourceSet Source set name (e.g., "jvmMain", "commonMain")
     * @return File pointing to the test source set directory
     */
    private fun resolveOutputDirectoryForSourceSet(sourceSet: String): File {
        // Map to test source set
        val testSourceSet = mapSourceSetToTestSourceSet(sourceSet)

        // Build output path from project base, NOT Gradle-provided dir
        // This avoids double nesting like: .../commonTest/kotlin/jvmTest/kotlin
        val projectBase = resolveProjectBaseDirectory()
        val outputPath = File(projectBase, "$testSourceSet/kotlin")

        messageCollector?.reportInfo(
            "Fakt: Source set routing - $sourceSet → $testSourceSet\n" +
                "  Output: ${outputPath.absolutePath}",
        )

        return outputPath
    }

    /**
     * Maps source set to corresponding test source set.
     *
     * **Mapping Rules**:
     * - {platform}Main → {platform}Test
     * - commonMain → commonTest
     * - jvmMain → jvmTest
     * - Already test → return as-is
     *
     * @param sourceSet Source set name
     * @return Corresponding test source set name
     */
    private fun mapSourceSetToTestSourceSet(sourceSet: String): String =
        when {
            // Already a test source set
            sourceSet.endsWith("Test") || sourceSet.endsWith("test") -> sourceSet

            // Main source sets → test source sets
            sourceSet.endsWith("Main") -> sourceSet.removeSuffix("Main") + "Test"
            sourceSet.endsWith("main") -> sourceSet.removeSuffix("main") + "test"

            // Special case: Android
            sourceSet == "android" || sourceSet == "androidMain" -> ANDROID_UNIT_TEST_TARGET

            // Fallback: append Test
            else -> "${sourceSet}Test"
        }

    /**
     * Attempts to use Gradle-provided output directory if available.
     *
     * @return File if Gradle output dir exists and is valid, null otherwise
     */
    private fun tryGradleProvidedOutputDir(): File? {
        if (outputDir == null) return null

        val dir = File(outputDir)
        return if (ensureDirectoryExists(dir)) {
            messageCollector?.reportInfo("Fakt: Using Gradle-provided output directory: ${dir.absolutePath}")
            dir
        } else {
            null
        }
    }

    /**
     * Uses intelligent source set mapping with fallback hierarchy.
     *
     * @param moduleName The module name to map
     * @return File pointing to the appropriate test source set directory
     */
    private fun tryIntelligentSourceSetMapping(moduleName: String): File {
        val primaryTarget = mapToTestSourceSet(moduleName)
        val baseDir = resolveBaseDirectory()
        val primaryDir = File(baseDir, "$primaryTarget/kotlin")

        return tryPrimaryTarget(moduleName, primaryTarget, primaryDir)
            ?: tryFallbackTargets(moduleName, primaryTarget, baseDir)
            ?: createPrimaryTarget(moduleName, primaryTarget, primaryDir)
    }

    /**
     * Attempts to use primary target directory.
     */
    private fun tryPrimaryTarget(
        moduleName: String,
        primaryTarget: String,
        primaryDir: File,
    ): File? =
        if (ensureDirectoryExists(primaryDir)) {
            reportMapping(moduleName, primaryTarget, primaryDir)
            primaryDir
        } else {
            null
        }

    /**
     * Attempts to use fallback target directories.
     */
    private fun tryFallbackTargets(
        moduleName: String,
        primaryTarget: String,
        baseDir: File,
    ): File? {
        val fallbackTargets = buildFallbackChain(moduleName)
        return fallbackTargets.firstNotNullOfOrNull { fallbackTarget ->
            val fallbackDir = File(baseDir, "$fallbackTarget/kotlin")
            if (ensureDirectoryExists(fallbackDir)) {
                reportFallback(moduleName, primaryTarget, fallbackTarget, fallbackDir)
                fallbackDir
            } else {
                null
            }
        }
    }

    /**
     * Creates primary target directory as last resort.
     */
    private fun createPrimaryTarget(
        moduleName: String,
        primaryTarget: String,
        primaryDir: File,
    ): File {
        primaryDir.mkdirs()
        reportCreated(moduleName, primaryTarget, primaryDir)
        return primaryDir
    }

    /**
     * Maps compilation context to appropriate test source set.
     * Implements comprehensive KMP source set mapping strategy based on official Kotlin conventions.
     * Supports all official KMP targets including hierarchical source sets and platform variants.
     *
     * @param moduleName The module name to analyze
     * @return The appropriate test source set name
     */
    private fun mapToTestSourceSet(moduleName: String): String {
        val normalizedName = moduleName.lowercase()

        return when {
            // Tier 1: Common source sets
            normalizedName.contains("commonmain") -> "commonTest"
            normalizedName.contains("commontest") -> "commonTest"

            // Tier 2: Platform categories (hierarchical)
            normalizedName.contains("nativemain") -> "nativeTest"
            normalizedName.contains("applemain") -> "appleTest"
            normalizedName.contains("linuxmain") -> "linuxTest"
            normalizedName.contains("mingwmain") -> "mingwTest"

            // Tier 3: Specific platforms
            normalizedName.contains("jvmmain") -> "jvmTest"
            normalizedName.contains("jsmain") -> "jsTest"
            normalizedName.contains("wasmjsmain") -> "wasmJsTest"
            normalizedName.contains("wasmwasimain") -> "wasmWasiTest"

            // Tier 4: Apple platforms
            normalizedName.contains("iosmain") -> "iosTest"
            normalizedName.contains("tvosmain") -> "tvosTest"
            normalizedName.contains("watchosmain") -> "watchosTest"
            normalizedName.contains("macosmain") -> "macosTest"

            // Tier 5: Platform variants (ALL official variants)
            normalizedName.contains("iosarm64main") -> "iosArm64Test"
            normalizedName.contains("iosx64main") -> "iosX64Test"
            normalizedName.contains("iossimulatorarm64main") -> "iosSimulatorArm64Test"
            normalizedName.contains("macosarm64main") -> "macosArm64Test"
            normalizedName.contains("macosx64main") -> "macosX64Test"
            normalizedName.contains("linuxarm64main") -> "linuxArm64Test"
            normalizedName.contains("linuxx64main") -> "linuxX64Test"
            normalizedName.contains("mingwx64main") -> "mingwX64Test"
            normalizedName.contains("tvosarm64main") -> "tvosArm64Test"
            normalizedName.contains("tvosx64main") -> "tvosX64Test"
            normalizedName.contains("tvossimulatorarm64main") -> "tvosSimulatorArm64Test"
            normalizedName.contains("watchosarm32main") -> "watchosArm32Test"
            normalizedName.contains("watchosarm64main") -> "watchosArm64Test"
            normalizedName.contains("watchosx64main") -> "watchosX64Test"
            normalizedName.contains("watchossimulatorarm64main") -> "watchosSimulatorArm64Test"
            normalizedName.contains("watchosdevicearm64main") -> "watchosDeviceArm64Test"
            normalizedName.contains("androidnativearm32main") -> "androidNativeArm32Test"
            normalizedName.contains("androidnativearm64main") -> "androidNativeArm64Test"
            normalizedName.contains("androidnativex64main") -> "androidNativeX64Test"
            normalizedName.contains("androidnativex86main") -> "androidNativeX86Test"

            // Tier 6: Android special cases
            normalizedName.contains("androidmain") -> ANDROID_UNIT_TEST_TARGET

            // Tier 7: Legacy JVM projects
            normalizedName.contains("main") && !normalizedName.contains("test") -> "test"

            // Default intelligent fallback patterns
            normalizedName.contains("jvm") -> "jvmTest"
            normalizedName.contains("android") -> "androidUnitTest"
            normalizedName.contains("ios") -> "iosTest"
            normalizedName.contains("js") -> "jsTest"
            normalizedName.contains("wasm") -> "wasmJsTest"
            normalizedName.contains("linux") -> "linuxTest"
            normalizedName.contains("macos") -> "macosTest"
            normalizedName.contains("mingw") -> "mingwTest"
            normalizedName.contains("tvos") -> "tvosTest"
            normalizedName.contains("watchos") -> "watchosTest"
            normalizedName.contains("native") -> "nativeTest"

            // Sample projects - since single-module uses JVM target, default to jvmTest
            normalizedName.contains("single-module") -> "jvmTest"
            normalizedName.contains("sample") -> "jvmTest" // Default samples to JVM test
            normalizedName.contains("test") -> "jvmTest"

            // Ultimate intelligent fallback
            else -> intelligentFallback(normalizedName)
        }
    }

    /**
     * Builds hierarchical fallback chain for KMP source sets.
     * Based on official Kotlin Multiplatform source set hierarchy.
     *
     * @param moduleName The module name to build fallback chain for
     * @return List of fallback source set names in priority order
     */
    private fun buildFallbackChain(moduleName: String): List<String> =
        when {
            // Apple platform hierarchy: platform -> apple -> native -> common
            moduleName.contains("ios") -> listOf("appleTest", "nativeTest", "commonTest")
            moduleName.contains("macos") -> listOf("appleTest", "nativeTest", "commonTest")
            moduleName.contains("tvos") -> listOf("appleTest", "nativeTest", "commonTest")
            moduleName.contains("watchos") -> listOf("appleTest", "nativeTest", "commonTest")

            // Linux platform hierarchy: linux -> native -> common
            moduleName.contains("linux") -> listOf("nativeTest", "commonTest")

            // Windows platform hierarchy: mingw -> native -> common
            moduleName.contains("mingw") -> listOf("nativeTest", "commonTest")

            // Android Native hierarchy: androidNative -> native -> common
            moduleName.contains("androidnative") -> listOf("nativeTest", "commonTest")

            // Android JVM hierarchy: android -> common
            moduleName.contains("android") -> listOf("commonTest")

            // JS/WASM hierarchy: js/wasm -> common
            moduleName.contains("js") -> listOf("commonTest")
            moduleName.contains("wasm") -> listOf("commonTest")

            // JVM hierarchy: jvm -> common
            moduleName.contains("jvm") -> listOf("commonTest")

            // Native fallback: native -> common
            moduleName.contains("native") -> listOf("commonTest")

            // Default fallback
            else -> listOf("commonTest")
        }

    companion object {
        /**
         * Default Android test target.
         * Currently defaults to androidUnitTest (unit tests) vs androidInstrumentedTest (integration tests).
         * Future enhancement: detect if project has androidInstrumentedTest configured.
         */
        private const val ANDROID_UNIT_TEST_TARGET = "androidUnitTest"
    }

    /**
     * Intelligent fallback strategy for unrecognized module names.
     * Uses pattern matching to determine most appropriate test source set.
     *
     * @param moduleName The module name to analyze
     * @return The best guess test source set name
     */
    private fun intelligentFallback(moduleName: String): String =
        when {
            moduleName.contains("android") -> "androidUnitTest"
            moduleName.contains("jvm") -> "jvmTest"
            moduleName.contains("js") -> "jsTest"
            moduleName.contains("wasm") -> "wasmJsTest"
            moduleName.contains("native") -> "nativeTest"
            moduleName.contains("ios") -> "iosTest"
            moduleName.contains("macos") -> "macosTest"
            moduleName.contains("linux") -> "linuxTest"
            moduleName.contains("mingw") -> "mingwTest"
            moduleName.contains("tvos") -> "tvosTest"
            moduleName.contains("watchos") -> "watchosTest"
            moduleName.contains("test") -> "commonTest"
            else -> "commonTest" // Ultimate fallback
        }

    /**
     * Resolves the project base directory (build/generated/fakt) WITHOUT source set.
     *
     * **Purpose**: Used when source set is extracted from IrFile to avoid double nesting.
     *
     * @return The base directory: /project/build/generated/fakt
     */
    private fun resolveProjectBaseDirectory(): File {
        // Try to extract project base from Gradle-provided outputDir
        if (outputDir != null) {
            val gradleDir = File(outputDir)
            // outputDir might be: /project/build/generated/fakt/commonTest/kotlin
            // We want: /project/build/generated/fakt
            val faktIndex = gradleDir.absolutePath.indexOf("/build/generated/fakt")
            if (faktIndex != -1) {
                val projectRoot = gradleDir.absolutePath.substring(0, faktIndex)
                return File(projectRoot, "build/generated/fakt")
            }
        }

        // Fallback: find project directory
        var dir = File(System.getProperty("user.dir"))
        if (dir.absolutePath.contains("daemon")) {
            dir = findProjectFromDaemon() ?: dir
        }
        dir = findProjectRoot(dir)
        return File(dir, "build/generated/fakt")
    }

    /**
     * Resolves the base directory for generated sources.
     *
     * **Note**: This may include source set path from Gradle. Use `resolveProjectBaseDirectory()`
     * when explicit source set is provided to avoid double nesting.
     *
     * @return The base directory for output
     */
    private fun resolveBaseDirectory(): File =
        when {
            outputDir != null -> File(outputDir)
            else -> {
                // Fallback: Try to find project directory by looking for build.gradle.kts
                var dir = File(System.getProperty("user.dir"))

                // If we're in a daemon directory, try to find the real project path
                if (dir.absolutePath.contains("daemon")) {
                    dir = findProjectFromDaemon() ?: dir
                }

                // Look for build.gradle.kts to confirm we're in the right directory
                dir = findProjectRoot(dir)
                File(dir, "build/generated/fakt")
            }
        }

    /**
     * Attempts to find project directory when running from Kotlin daemon.
     */
    private fun findProjectFromDaemon(): File? =
        try {
            val classLoader = this::class.java.classLoader
            val resourceUrl = classLoader.getResource("")
            if (resourceUrl != null) {
                val path = File(resourceUrl.path)
                // Navigate up from build/classes/kotlin/main to project root
                var parent = path
                while (parent.parentFile != null && !File(parent, "build.gradle.kts").exists()) {
                    parent = parent.parentFile
                }
                if (File(parent, "build.gradle.kts").exists()) parent else null
            } else {
                null
            }
        } catch (e: java.io.IOException) {
            messageCollector?.reportInfo("Fakt: IOException finding project from daemon: ${e.message}")
            null
        } catch (e: SecurityException) {
            messageCollector?.reportInfo("Fakt: SecurityException finding project from daemon: ${e.message}")
            null
        }

    /**
     * Finds the project root by looking for build.gradle.kts.
     */
    private fun findProjectRoot(startDir: File): File {
        if (File(startDir, "build.gradle.kts").exists()) {
            return startDir
        }

        var parent = startDir.parentFile
        while (parent != null && !File(parent, "build.gradle.kts").exists()) {
            parent = parent.parentFile
        }
        return parent ?: startDir
    }

    /**
     * Ensures a directory exists and can be written to.
     * Returns true if directory is available, false otherwise.
     *
     * @param dir The directory to check/create
     * @return true if directory is usable, false otherwise
     */
    private fun ensureDirectoryExists(dir: File): Boolean =
        try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir.exists() && dir.isDirectory && dir.canWrite()
        } catch (e: java.io.IOException) {
            messageCollector?.reportInfo("Fakt: Cannot access directory ${dir.absolutePath}: ${e.message}")
            false
        } catch (e: SecurityException) {
            messageCollector?.reportInfo("Fakt: Security restriction on directory ${dir.absolutePath}: ${e.message}")
            false
        }

    /**
     * Reports successful primary target mapping.
     */
    private fun reportMapping(
        moduleName: String,
        primaryTarget: String,
        dir: File,
    ) {
        messageCollector?.reportInfo("Fakt: Module '$moduleName' -> Primary target '$primaryTarget'")
        messageCollector?.reportInfo("Fakt: Output directory: ${dir.absolutePath}")
    }

    /**
     * Reports fallback target usage.
     */
    private fun reportFallback(
        moduleName: String,
        primaryTarget: String,
        fallbackTarget: String,
        dir: File,
    ) {
        messageCollector?.reportInfo(
            "Fakt: Module '$moduleName' -> Primary target '$primaryTarget' not available, " +
                "using fallback '$fallbackTarget'",
        )
        messageCollector?.reportInfo("Fakt: Output directory: ${dir.absolutePath}")
    }

    /**
     * Reports creation of new primary target directory.
     */
    private fun reportCreated(
        moduleName: String,
        primaryTarget: String,
        dir: File,
    ) {
        messageCollector?.reportInfo(
            "Fakt: Module '$moduleName' -> Created primary target '$primaryTarget' " +
                "(fallbacks unavailable)",
        )
        messageCollector?.reportInfo("Fakt: Output directory: ${dir.absolutePath}")
    }

    private fun MessageCollector.reportInfo(message: String) {
        this.report(org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO, message)
    }
}
