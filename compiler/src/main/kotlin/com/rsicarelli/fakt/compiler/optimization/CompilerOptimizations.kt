// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.optimization

import com.rsicarelli.fakt.compiler.types.TypeInfo

/**
 * Provides compiler optimization capabilities including custom annotation support and incremental compilation.
 *
 * This interface enables efficient fake generation by supporting:
 * - Custom annotation detection for enterprise flexibility
 * - Type indexing for O(log n) lookup performance
 * - Incremental compilation to skip unchanged types
 * - Change detection based on type signatures
 *
 * Companies can configure their own annotations instead of being limited to the default `@Fake` annotation,
 * providing better ownership and protection against breaking changes in the Fakt library.
 *
 * @since 1.0.0
 * @see TypeInfo
 */
interface CompilerOptimizations {
    /**
     * Checks if the optimization system is configured to process the specified annotation.
     *
     * @param annotation The fully qualified annotation name to check
     * @return `true` if the annotation is configured for processing, `false` otherwise
     */
    fun isConfiguredFor(annotation: String): Boolean

    /**
     * Indexes a type for efficient lookup and processing.
     *
     * This method stores type metadata to enable fast annotation-based discovery
     * and change detection during subsequent compilations.
     *
     * @param type The type information to index
     */
    fun indexType(type: TypeInfo)

    /**
     * Finds all indexed types that are annotated with the specified annotation.
     *
     * @param annotation The fully qualified annotation name to search for
     * @return List of types that have the specified annotation
     */
    fun findTypesWithAnnotation(annotation: String): List<TypeInfo>

    /**
     * Determines if a type needs to be regenerated based on signature changes.
     *
     * This method enables incremental compilation by detecting when a type's
     * signature has changed since the last generation.
     *
     * @param type The type to check for changes
     * @return `true` if the type needs regeneration, `false` if it can be skipped
     */
    fun needsRegeneration(type: TypeInfo): Boolean

    /**
     * Records that a type has been successfully generated.
     *
     * This method updates the internal cache to mark a type as up-to-date,
     * enabling future incremental compilation optimizations.
     *
     * @param type The type that was successfully generated
     */
    fun recordGeneration(type: TypeInfo)

    companion object {
        /**
         * Creates a new [CompilerOptimizations] instance with the specified configuration.
         *
         * Simple in-memory implementation without incremental compilation persistence.
         *
         * @param fakeAnnotations List of fully qualified annotation names to process.
         *                       Defaults to `["com.rsicarelli.fakt.Fake"]`
         * @param outputDir Optional output directory (unused in simple implementation)
         * @return A new optimization instance configured for the specified annotations
         */
        operator fun invoke(
            fakeAnnotations: List<String> = listOf("com.rsicarelli.fakt.Fake"),
            outputDir: String? = null,
        ): CompilerOptimizations =
            object : CompilerOptimizations {
                private val indexedTypes = mutableListOf<TypeInfo>()
                private val generatedSignatures = mutableSetOf<String>()

                override fun isConfiguredFor(annotation: String): Boolean = annotation in fakeAnnotations

                override fun indexType(type: TypeInfo) {
                    indexedTypes.add(type)
                }

                override fun findTypesWithAnnotation(annotation: String): List<TypeInfo> =
                    indexedTypes.filter { type ->
                        type.annotations.any { it == annotation }
                    }

                override fun needsRegeneration(type: TypeInfo): Boolean {
                    // Always regenerate in simple mode (no incremental compilation)
                    return true
                }

                override fun recordGeneration(type: TypeInfo) {
                    generatedSignatures.add(type.signature)
                }
            }
    }
}
