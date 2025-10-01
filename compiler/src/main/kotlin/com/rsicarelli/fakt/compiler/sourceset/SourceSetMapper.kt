// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.sourceset

import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import java.io.File

/**
 * Handles source set mapping and output directory resolution for Kotlin Multiplatform projects.
 * Provides intelligent fallback strategies for different KMP target configurations.
 *
 * @since 1.0.0
 */
internal class SourceSetMapper(
    private val outputDir: String?,
    private val messageCollector: MessageCollector?
) {

    /**
     * Get generated sources directory with intelligent source set mapping and fallback strategy.
     * Maps source locations to appropriate test source sets with hierarchical fallback:
     * - commonMain -> commonTest
     * - jvmMain -> jvmTest
     * - androidMain -> androidUnitTest
     * - iosMain -> iosTest (with fallback to appleTest -> nativeTest -> commonTest)
     * - main (JVM-only) -> test
     *
     * @param moduleFragment The IR module fragment to analyze
     * @return File pointing to the appropriate generated sources directory
     */
    fun getGeneratedSourcesDir(moduleFragment: IrModuleFragment): File {
        val moduleName = moduleFragment.name.asString().lowercase()

        // If outputDir is explicitly provided by Gradle plugin, use it directly
        // The Gradle plugin already handles the source set mapping (main -> test)
        if (outputDir != null) {
            val dir = File(outputDir)
            if (ensureDirectoryExists(dir)) {
                messageCollector?.reportInfo("KtFakes: Using Gradle-provided output directory: ${dir.absolutePath}")
                return dir
            }
        }

        // Otherwise, use intelligent source set mapping for auto-detection
        val primaryTarget = mapToTestSourceSet(moduleName)
        val baseDir = resolveBaseDirectory()

        // Try primary target first
        val primaryDir = File(baseDir, "$primaryTarget/kotlin")
        if (ensureDirectoryExists(primaryDir)) {
            reportMapping(moduleName, primaryTarget, primaryDir)
            return primaryDir
        }

        // Fall back through hierarchy if primary target fails
        val fallbackTargets = buildFallbackChain(moduleName)
        for (fallbackTarget in fallbackTargets) {
            val fallbackDir = File(baseDir, "$fallbackTarget/kotlin")
            if (ensureDirectoryExists(fallbackDir)) {
                reportFallback(moduleName, primaryTarget, fallbackTarget, fallbackDir)
                return fallbackDir
            }
        }

        // Create primary target if all fallbacks fail
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
            normalizedName.contains("androidmain") -> resolveAndroidTestTarget(normalizedName)

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
            normalizedName.contains("sample") -> "jvmTest"  // Default samples to JVM test
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
    private fun buildFallbackChain(moduleName: String): List<String> {
        return when {
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
    }

    /**
     * Resolves Android test target based on project configuration.
     * Defaults to androidUnitTest (unit tests) vs androidInstrumentedTest (integration tests).
     *
     * @param moduleName The Android module name
     * @return The appropriate Android test source set
     */
    private fun resolveAndroidTestTarget(moduleName: String): String {
        // Strategy: Default to androidUnitTest, can be enhanced later for instrumented tests
        // Future enhancement: detect if project has androidInstrumentedTest configured
        return "androidUnitTest" // vs "androidInstrumentedTest"
    }

    /**
     * Intelligent fallback strategy for unrecognized module names.
     * Uses pattern matching to determine most appropriate test source set.
     *
     * @param moduleName The module name to analyze
     * @return The best guess test source set name
     */
    private fun intelligentFallback(moduleName: String): String {
        return when {
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
    }

    /**
     * Resolves the base directory for generated sources.
     *
     * @return The base directory for output
     */
    private fun resolveBaseDirectory(): File {
        return when {
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
                File(dir, "build/generated/ktfake")
            }
        }
    }

    /**
     * Attempts to find project directory when running from Kotlin daemon.
     */
    private fun findProjectFromDaemon(): File? {
        return try {
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
            } else null
        } catch (e: Exception) {
            null
        }
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
    private fun ensureDirectoryExists(dir: File): Boolean {
        return try {
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir.exists() && dir.isDirectory && dir.canWrite()
        } catch (e: Exception) {
            messageCollector?.reportInfo("KtFakes: Cannot access directory ${dir.absolutePath}: ${e.message}")
            false
        }
    }

    /**
     * Reports successful primary target mapping.
     */
    private fun reportMapping(moduleName: String, primaryTarget: String, dir: File) {
        messageCollector?.reportInfo("KtFakes: Module '$moduleName' -> Primary target '$primaryTarget'")
        messageCollector?.reportInfo("KtFakes: Output directory: ${dir.absolutePath}")
    }

    /**
     * Reports fallback target usage.
     */
    private fun reportFallback(moduleName: String, primaryTarget: String, fallbackTarget: String, dir: File) {
        messageCollector?.reportInfo(
            "KtFakes: Module '$moduleName' -> Primary target '$primaryTarget' not available, using fallback '$fallbackTarget'"
        )
        messageCollector?.reportInfo("KtFakes: Output directory: ${dir.absolutePath}")
    }

    /**
     * Reports creation of new primary target directory.
     */
    private fun reportCreated(moduleName: String, primaryTarget: String, dir: File) {
        messageCollector?.reportInfo("KtFakes: Module '$moduleName' -> Created primary target '$primaryTarget' (fallbacks unavailable)")
        messageCollector?.reportInfo("KtFakes: Output directory: ${dir.absolutePath}")
    }

    private fun MessageCollector.reportInfo(message: String) {
        this.report(org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.INFO, message)
    }
}
