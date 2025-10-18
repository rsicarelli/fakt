// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

/**
 * Discovers and builds complete source set context for a compilation.
 *
 * **Purpose**: Integration point that combines all Week 1 utilities:
 * - CompilationClassifier → Determine test vs main
 * - SourceSetGraphTraversal → Build source set hierarchy
 * - SourceSetContext → Package for compiler plugin
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
 *
 * **Usage**:
 * ```kotlin
 * val context = SourceSetDiscovery.buildContext(
 *     compilation = target.compilations.getByName("test"),
 *     buildDir = project.layout.buildDirectory.get().asFile.absolutePath
 * )
 * // context contains all metadata needed by compiler plugin
 * ```
 *
 * @since 1.1.0
 */
internal object SourceSetDiscovery {
    private const val MAIN_SUFFIX_LENGTH = 4 // Length of "Main" suffix

    /**
     * Maps a main compilation name to its corresponding test source set.
     *
     * **Examples**:
     * - "main" → "test"
     * - "jvmMain" → "jvmTest"
     * - "commonMain" → "commonTest"
     * - "iosX64Main" → "iosX64Test"
     * - "test" → "test" (already a test compilation)
     * - "jvmTest" → "jvmTest" (already a test compilation)
     *
     * @param compilationName The compilation name to map
     * @return The corresponding test source set name
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
     * **Algorithm**:
     * 1. Classify compilation (test vs main) using CompilationClassifier
     * 2. Get default source set from compilation
     * 3. Traverse source set graph to find all parents
     * 4. Extract platform type and target metadata
     * 5. Build hierarchy map for compiler plugin
     * 6. Generate output directory path
     * 7. Package into SourceSetContext
     *
     * @param compilation The compilation to analyze
     * @param buildDir The project's build directory path
     * @return Complete SourceSetContext ready for serialization
     */
    fun buildContext(
        compilation: KotlinCompilation<*>,
        buildDir: String,
    ): SourceSetContext {
        // 1. Classify compilation (test vs main)
        val isTest = CompilationClassifier.isTestCompilation(compilation)

        // 2. Get default source set
        val defaultSourceSet = compilation.defaultSourceSet

        // 3. Traverse source set graph to get all parents
        val allSourceSets = SourceSetGraphTraversal.getAllParentSourceSets(defaultSourceSet)

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
        val targetName = compilation.target.targetName
        val platformType = compilation.target.platformType.name

        // 7. Generate output directory
        // Always output to test source sets since fakes are only used in tests
        //
        // CRITICAL: For KMP projects, if commonMain exists in the hierarchy,
        // ALWAYS generate in commonTest, not platform-specific test source sets.
        // This ensures fakes are visible to ALL platform tests.
        //
        // Example: Interface in commonMain → generate fake in commonTest
        //          Tests in commonTest, jvmTest, iosTest can all see it
        val hasCommonMain = allSourceSets.any { it.name == "commonMain" }
        val testSourceSet =
            if (hasCommonMain) {
                "commonTest"
            } else {
                // Non-KMP or platform-specific interfaces: use platform test source set
                mapToTestSourceSet(defaultSourceSet.name)
            }
        val outputDirectory = "$buildDir/generated/fakt/$testSourceSet/kotlin"

        // 8. Package into context
        return SourceSetContext(
            compilationName = compilation.name,
            targetName = targetName,
            platformType = platformType,
            isTest = isTest,
            defaultSourceSet = defaultSourceSetInfo,
            allSourceSets = sourceSetInfos,
            outputDirectory = outputDirectory,
        )
    }
}
