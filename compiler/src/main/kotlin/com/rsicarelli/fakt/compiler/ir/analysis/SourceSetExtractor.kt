// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile

/**
 * Extracts source set information from IrClass by analyzing the file path.
 *
 * **Purpose**:
 * In KMP compilations, a single compilation (e.g., jvmMain) can include files from multiple source sets:
 * - Files from jvmMain/kotlin/ (platform-specific)
 * - Files from commonMain/kotlin/ (shared code visible via dependsOn hierarchy)
 *
 * We need to determine which source set each IrClass came from to route fakes correctly:
 * - jvmMain files → jvmTest/
 * - commonMain files → commonTest/ (even when compiled in jvmMain!)
 *
 * **Solution**:
 * Extract source set from the actual file path by parsing the directory structure.
 * This is the SOURCE OF TRUTH - no package name guessing, no manual mappings.
 *
 * **Examples**:
 * ```
 * /project/api/src/jvmMain/kotlin/Database.kt → "jvmMain"
 * /project/api/src/commonMain/kotlin/Network.kt → "commonMain"
 * /project/api/src/iosArm64Main/kotlin/Service.kt → "iosArm64Main"
 * ```
 *
 * @since 1.0.0
 */
object SourceSetExtractor {
    /**
     * Extracts the source set name from an IrClass by analyzing its file path.
     *
     * **Algorithm**:
     * 1. Get IrFile from IrClass.parent hierarchy
     * 2. Extract file path from IrFile.fileEntry.name
     * 3. Parse path to extract source set: /path/to/src/{sourceSet}/kotlin/File.kt
     *
     * @param irClass The IR class to extract source set from
     * @return Source set name (e.g., "jvmMain", "commonMain") or null if path is invalid
     */
    fun extractSourceSet(irClass: IrClass): String? {
        // Navigate to IrFile in parent hierarchy
        var current = irClass.parent
        while (current != null) {
            when (current) {
                is IrFile -> return extractSourceSetFromPath(current.fileEntry.name)
                is IrDeclaration -> current = current.parent
                else -> break
            }
        }
        return null
    }

    /**
     * Extracts source set name from a file path.
     *
     * **Path Structure**:
     * - Standard Kotlin: `/path/to/project/src/{sourceSet}/kotlin/File.kt`
     * - Gradle build: `/path/to/project/build/classes/kotlin/{sourceSet}/File.kt`
     *
     * **Supported Source Sets**:
     * - Main source sets: commonMain, jvmMain, iosMain, jsMain, etc.
     * - Test source sets: commonTest, jvmTest, iosTest, etc.
     * - Custom source sets: integrationTest, e2eTest, etc.
     * - Platform variants: iosArm64Main, macosX64Main, linuxX64Main, etc.
     *
     * @param filePath The absolute file path to parse
     * @return Source set name or null if path doesn't match expected structure
     */
    fun extractSourceSetFromPath(filePath: String): String? {
        val normalizedPath = filePath.replace('\\', '/')
        val srcIndex = normalizedPath.indexOf("/src/")
        val buildIndex = normalizedPath.indexOf("/build/")

        return when {
            srcIndex != -1 -> {
                val afterSrc = normalizedPath.substring(srcIndex + "/src/".length)
                val sourceSet = afterSrc.substringBefore('/')
                sourceSet.takeIf { it.isNotBlank() && it != "kotlin" && it != "java" }
            }
            buildIndex != -1 -> extractFromBuildPath(normalizedPath, buildIndex)
            else -> null
        }
    }

    /**
     * Extracts source set from build output path.
     *
     * **Build Path Structure**:
     * `/build/classes/kotlin/{sourceSet}/main/File.class`
     * `/build/generated/source/kapt/{sourceSet}/File.kt`
     *
     * @param path Normalized path
     * @param buildIndex Index of /build/ in path
     * @return Source set name or null
     */
    private fun extractFromBuildPath(
        path: String,
        buildIndex: Int,
    ): String? {
        val afterBuild = path.substring(buildIndex + "/build/".length)

        // Try to find source set in build path
        // Common patterns:
        // - /build/classes/kotlin/{sourceSet}/
        // - /build/generated/{sourceSet}/
        val segments = afterBuild.split('/')

        // Look for source set-like segment (ends with Main or Test)
        val sourceSet =
            segments.firstOrNull { segment ->
                segment.endsWith("Main") ||
                    segment.endsWith("Test") ||
                    segment.endsWith("main") ||
                    segment.endsWith("test")
            }

        return sourceSet
    }

    /**
     * Maps source set to its corresponding test source set.
     *
     * **Mapping Rules**:
     * - {platform}Main → {platform}Test
     * - commonMain → commonTest
     * - jvmMain → jvmTest
     * - Already a test source set → return as-is
     *
     * @param sourceSet The source set name (e.g., "jvmMain", "commonMain")
     * @return Corresponding test source set (e.g., "jvmTest", "commonTest")
     */
    fun mapToTestSourceSet(sourceSet: String): String =
        when {
            // Already a test source set
            sourceSet.endsWith("Test") || sourceSet.endsWith("test") -> sourceSet

            // Main source sets → corresponding test source sets
            sourceSet.endsWith("Main") -> sourceSet.removeSuffix("Main") + "Test"
            sourceSet.endsWith("main") -> sourceSet.removeSuffix("main") + "test"

            // Fallback: assume it's a main source set and append Test
            else -> "${sourceSet}Test"
        }
}
