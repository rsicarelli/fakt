// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet

/**
 * Utilities for traversing the KotlinSourceSet dependsOn graph.
 *
 * **Purpose**: Replace hardcoded source set fallback chains with programmatic BFS traversal.
 *
 * **Key Concept**: In Kotlin Multiplatform, source sets form a Directed Acyclic Graph (DAG)
 * via the `dependsOn` relationship. For example:
 * ```
 * iosX64Main.dependsOn(iosMain)
 * iosMain.dependsOn(appleMain)
 * appleMain.dependsOn(nativeMain)
 * nativeMain.dependsOn(commonMain)
 * ```
 *
 * This forms a hierarchy where each source set can see code from all its parents.
 * The compiler needs to know the COMPLETE hierarchy to properly resolve types and symbols.
 *
 * **Algorithm**: Breadth-First Search (BFS)
 * - Visits all source sets in the dependency graph
 * - Uses a Set to avoid duplicates (handles diamond dependencies)
 * - Includes the starting source set in the result
 *
 * **Benefits**:
 * - No hardcoded source set names
 * - Automatically handles custom hierarchies
 * - Works with default hierarchy template or custom setup
 * - Handles diamond dependencies correctly
 */
internal object SourceSetGraphTraversal {
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
     * Result: Set(iosMain, nativeMain, appleMain, commonMain)  // No duplicates!
     * ```
     *
     * @param sourceSet The starting source set to traverse from
     * @return Set of all source sets in the hierarchy (including starting set)
     */
    fun getAllParentSourceSets(sourceSet: KotlinSourceSet): Set<KotlinSourceSet> {
        val allParents = mutableSetOf<KotlinSourceSet>()
        val queue = ArrayDeque<KotlinSourceSet>()

        // Start with the initial source set
        queue.add(sourceSet)
        allParents.add(sourceSet)

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

    /**
     * Build a map suitable for serialization to compiler plugin.
     * Maps each source set name to its direct parent names.
     *
     * **Structure**:
     * ```kotlin
     * {
     *   "jvmMain" -> ["commonMain"],
     *   "commonMain" -> []
     * }
     * ```
     *
     * **Purpose**: This map is serialized and passed to the compiler plugin,
     * allowing it to understand the project's source set hierarchy without
     * any hardcoded patterns.
     *
     * @param sourceSet The source set to build hierarchy map for
     * @return Map from source set name to list of direct parent names (sorted for determinism)
     */
    fun buildHierarchyMap(sourceSet: KotlinSourceSet): Map<String, List<String>> {
        val allSourceSets = getAllParentSourceSets(sourceSet)

        return allSourceSets.associate { current ->
            val parentNames =
                current.dependsOn
                    .map { it.name }
                    .sorted() // Deterministic ordering for serialization

            current.name to parentNames
        }
    }
}
