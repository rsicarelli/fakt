// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.optimization

import java.io.File

/**
 * Persistent cache for interface signatures to enable incremental compilation.
 *
 * This class handles loading and saving of interface signatures between compilation
 * sessions, allowing the compiler to skip regenerating unchanged interfaces.
 *
 * @property outputDir Directory where cache files are stored, or null for in-memory only
 *
 * @since 1.0.0
 */
internal class SignatureCache(
    private val outputDir: String?
) {
    /** In-memory cache of interface signatures: typeKey -> signature */
    private val signatures = mutableMapOf<String, String>()

    init {
        loadSignatures()
    }

    /**
     * Gets the cached signature for a given type.
     *
     * @param typeKey Unique identifier for the type (fullyQualifiedName@fileName)
     * @return Cached signature or null if not found
     */
    fun getSignature(typeKey: String): String? {
        return signatures[typeKey]
    }

    /**
     * Stores a signature for a type in the cache.
     *
     * @param typeKey Unique identifier for the type
     * @param signature Interface signature for change detection
     */
    fun putSignature(typeKey: String, signature: String) {
        signatures[typeKey] = signature
    }

    /**
     * Checks if a signature exists in the cache.
     *
     * @param typeKey Unique identifier for the type
     * @return true if signature is cached, false otherwise
     */
    fun hasSignature(typeKey: String): Boolean {
        return typeKey in signatures
    }

    /**
     * Gets the number of cached signatures.
     *
     * @return Count of signatures in cache
     */
    fun size(): Int {
        return signatures.size
    }

    /**
     * Saves all cached signatures to persistent storage.
     *
     * If outputDir is null, this operation is a no-op.
     * The cache file format is simple: each line contains "typeKey=signature"
     */
    fun save() {
        if (outputDir == null) return

        try {
            val cacheFile = File(outputDir, CACHE_FILE_NAME)
            cacheFile.parentFile?.mkdirs()

            val content = signatures.entries.joinToString("\n") { (key, signature) ->
                "$key=$signature"
            }

            cacheFile.writeText(content)
            println("KtFakes: Saved ${signatures.size} signatures to cache")
        } catch (e: Exception) {
            println("KtFakes: Failed to save signature cache: ${e.message}")
        }
    }

    /**
     * Clears all cached signatures.
     */
    fun clear() {
        signatures.clear()
    }

    /**
     * Gets all cached signatures as a read-only map.
     */
    fun getAllSignatures(): Map<String, String> {
        return signatures.toMap()
    }

    /**
     * Loads signatures from persistent storage if available.
     */
    private fun loadSignatures() {
        if (outputDir == null) return

        try {
            val cacheFile = File(outputDir, CACHE_FILE_NAME)
            if (!cacheFile.exists()) return

            cacheFile.readLines().forEach { line ->
                val parts = line.split("=", limit = 2)
                if (parts.size == 2) {
                    signatures[parts[0]] = parts[1]
                }
            }

            if (signatures.isNotEmpty()) {
                println("KtFakes: Loaded ${signatures.size} cached signatures for incremental compilation")
            }
        } catch (e: Exception) {
            println("KtFakes: Failed to load signature cache: ${e.message}")
        }
    }

    companion object {
        private const val CACHE_FILE_NAME = "ktfakes-signatures.cache"
    }
}
