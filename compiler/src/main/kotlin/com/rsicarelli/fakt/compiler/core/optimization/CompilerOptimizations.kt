// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.optimization

import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.core.types.TypeInfo
import java.io.File

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

    /**
     * Returns the number of signatures currently loaded in the cache.
     *
     * @return The size of the cache
     */
    fun cacheSize(): Int

    companion object {
        /**
         * Returns a [CompilerOptimizations] instance with file-based caching.
         *
         * **FILE-BASED CACHE**: Uses a file in the build directory to persist generated signatures
         * across multiple compilation tasks (KMP targets). This prevents redundant generation when
         * multiple targets (jvm, js, native, metadata, etc.) compile the same interface.
         *
         * @param fakeAnnotations List of fully qualified annotation names to process.
         *                       Defaults to `["com.rsicarelli.fakt.Fake"]`
         * @param outputDir Output directory for generated code (used to determine cache file location)
         * @return An optimization instance configured for the specified annotations with file-based caching
         */
        operator fun invoke(
            fakeAnnotations: List<String> = listOf("com.rsicarelli.fakt.Fake"),
            outputDir: String? = null,
            logger: FaktLogger,
        ): CompilerOptimizations {
            return object : CompilerOptimizations {
                private val indexedTypes = mutableListOf<TypeInfo>()

                // File-based cache for signatures (shared across compilation tasks)
                private val cacheFile: File? =
                    outputDir?.let { dir ->
                        // Use build/generated/fakt/cache/ for all caches
                        // This ensures the cache is shared across all source sets
                        File(dir)
                            .parentFile
                            ?.parentFile
                            ?.resolve("cache")
                            ?.resolve("ir-signatures.txt")
                            ?.also {
                                it.parentFile?.mkdirs()
                            }
                    }

                // Load previously generated signatures from file
                private val generatedSignatures: MutableSet<String> = loadSignaturesFromFile()

                private fun loadSignaturesFromFile(): MutableSet<String> {
                    val signatures = mutableSetOf<String>()
                    if (cacheFile?.exists() == true) {
                        try {
                            cacheFile.readLines().forEach { line ->
                                if (line.isNotBlank()) {
                                    signatures.add(line.trim())
                                }
                            }
                        } catch (e: Exception) {
                            logger.warn(
                                "Cache unavailable: Failed to load signatures from ${cacheFile.name}: ${e.message}",
                            )
                            logger.debug("Full cache load error: ${e.stackTraceToString()}")
                        }
                    }
                    return signatures
                }

                private fun saveSignatureToFile(signature: String) {
                    if (cacheFile == null) return

                    try {
                        // Append signature to file (synchronized to avoid concurrent writes)
                        synchronized(cacheFile) {
                            cacheFile.appendText("$signature\n")
                        }
                    } catch (e: Exception) {
                        logger.warn("Cache update failed: Cannot write signature to ${cacheFile.name}: ${e.message}")
                        logger.debug("Full cache write error: ${e.stackTraceToString()}")
                    }
                }

                override fun isConfiguredFor(annotation: String): Boolean = annotation in fakeAnnotations

                override fun indexType(type: TypeInfo) {
                    indexedTypes.add(type)
                }

                override fun findTypesWithAnnotation(annotation: String): List<TypeInfo> =
                    indexedTypes.filter { type ->
                        type.annotations.any { it == annotation }
                    }

                override fun needsRegeneration(type: TypeInfo): Boolean {
                    // Check file-based cache to skip regeneration across compilation tasks
                    // This prevents redundant generation when multiple KMP targets compile the same interface
                    return type.signature !in generatedSignatures
                }

                override fun recordGeneration(type: TypeInfo) {
                    // Only save to file if this is a new signature (Set.add returns true for new)
                    if (generatedSignatures.add(type.signature)) {
                        saveSignatureToFile(type.signature)
                    }
                }

                override fun cacheSize(): Int = generatedSignatures.size
            }
        }
    }
}
