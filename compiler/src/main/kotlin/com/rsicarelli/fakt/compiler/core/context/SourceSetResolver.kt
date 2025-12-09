// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.context

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo

/**
 * Resolves source set hierarchies from compiler context.
 *
 * **Key Responsibilities**:
 * 1. Resolve source set by name from context
 * 2. Traverse parent hierarchy
 * 3. Provide default source set
 * 4. Support multi-parent source sets (KMP shared source sets)
 *
 * @param context The source set context from Gradle plugin (via CommandLineProcessor)
 */
class SourceSetResolver(
    private val context: SourceSetContext,
) {
    /**
     * Resolves a source set by name.
     *
     * @param sourceSetName The name of the source set to resolve
     * @return SourceSetInfo if found, null otherwise
     */
    fun resolveSourceSet(sourceSetName: String): SourceSetInfo? =
        context.allSourceSets.firstOrNull {
            it.name == sourceSetName
        }

    /**
     * Gets the default source set for this compilation.
     *
     * @return The default source set from context
     */
    fun getDefaultSourceSet(): SourceSetInfo = context.defaultSourceSet

    /**
     * Gets all parent source sets for a given source set (excluding the source set itself).
     * Traverses the full hierarchy recursively.
     *
     * @param sourceSetName The name of the source set
     * @return List of all parent source sets in the hierarchy
     */
    fun getAllParentSourceSets(sourceSetName: String): List<SourceSetInfo> {
        val result = mutableSetOf<SourceSetInfo>()
        val visited = mutableSetOf<String>()

        fun traverse(name: String) {
            val sourceSet = resolveSourceSet(name) ?: return

            sourceSet.parents.forEach { parentName ->
                if (parentName !in visited) {
                    visited.add(parentName)
                    val parent = resolveSourceSet(parentName)
                    if (parent != null) {
                        result.add(parent)
                        traverse(parentName)
                    }
                }
            }
        }

        traverse(sourceSetName)
        return result.toList()
    }

    /**
     * Gets all source sets in the context.
     *
     * @return List of all source sets
     */
    fun getAllSourceSets(): List<SourceSetInfo> = context.allSourceSets

    /**
     * Checks if the current compilation is for test source sets.
     *
     * @return true if this is a test compilation
     */
    fun isTestSourceSet(): Boolean = context.isTest
}
