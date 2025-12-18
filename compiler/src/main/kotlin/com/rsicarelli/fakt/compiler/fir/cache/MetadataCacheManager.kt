// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.cache

import com.rsicarelli.fakt.compiler.api.FirMetadataCache
import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.fir.metadata.FirMetadataStorage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages FIR metadata cache for cross-compilation optimization in KMP projects.
 *
 * ## Purpose
 *
 * In KMP projects, identical FIR analysis runs N times (once per platform).
 * This manager enables caching analysis results from metadata compilation so
 * platform compilations can skip redundant analysis.
 *
 * ## Operation Modes
 *
 * - **Producer Mode** (`metadataOutputPath` set): Metadata compilation writes cache after FIR analysis
 * - **Consumer Mode** (`metadataCachePath` set): Platform compilations read cache and skip FIR analysis
 *
 * ## Thread Safety
 *
 * Uses atomic operations for safe concurrent access from parallel Gradle workers.
 *
 * ## Usage
 *
 * ```kotlin
 * // Producer mode (in FakeInterfaceChecker after FIR analysis)
 * if (cacheManager.isProducerMode) {
 *     // ... perform FIR analysis ...
 *     cacheManager.writeCache(storage)
 * }
 *
 * // Consumer mode (at start of FakeInterfaceChecker)
 * if (cacheManager.tryLoadCache(storage)) {
 *     // Cache loaded successfully, skip FIR analysis
 *     return
 * }
 * ```
 *
 * @property metadataOutputPath Path to write cache (producer mode) - null if consumer
 * @property metadataCachePath Path to read cache (consumer mode) - null if producer
 */
class MetadataCacheManager(
    private val metadataOutputPath: String?,
    private val metadataCachePath: String?,
    private val logger: FaktLogger,
) {
    private val cacheLoaded = AtomicBoolean(false)
    private var lastWriteResult: CacheWriteResult? = null
    private var savedFirTimeNanos: Long = 0L

    /**
     * True if operating in producer mode (metadata compilation writes cache).
     */
    val isProducerMode: Boolean get() = metadataOutputPath != null

    /**
     * True if operating in consumer mode (platform compilation reads cache).
     */
    val isConsumerMode: Boolean get() = metadataCachePath != null

    /**
     * True if caching is enabled (either producer or consumer mode).
     */
    val isEnabled: Boolean get() = isProducerMode || isConsumerMode

    /**
     * Try to load cached metadata into storage.
     *
     * Called at the start of FIR phase before analysis.
     * If cache is valid and loaded, FIR analysis can be skipped.
     *
     * @param storage FirMetadataStorage to populate from cache
     * @return true if cache was loaded successfully and FIR analysis can be skipped
     */
    fun tryLoadCache(storage: FirMetadataStorage): Boolean {
        if (!isConsumerMode) return false
        if (cacheLoaded.get()) return true // Already loaded - no log needed

        val cachePath = metadataCachePath ?: return false
        val cache = MetadataCacheSerializer.deserialize(cachePath) ?: return false

        if (!validateCache(cache)) {
            return false
        }

        // Load interfaces into storage and track cache hits
        cache.interfaces.forEach { serializable ->
            val validated = MetadataCacheSerializer.toValidated(serializable)
            storage.storeInterface(validated)
            storage.incrementInterfaceCacheHits()
        }

        // Load classes into storage and track cache hits
        cache.classes.forEach { serializable ->
            val validated = MetadataCacheSerializer.toValidated(serializable)
            storage.storeClass(validated)
            storage.incrementClassCacheHits()
        }

        // Store saved FIR time for unified logging (will be shown in Fakt Trace tree)
        savedFirTimeNanos = cache.totalFirTimeNanos

        cacheLoaded.set(true)
        return true
    }

    /**
     * Get the saved FIR time from cache load.
     *
     * Returns the total FIR validation time that was saved by loading from cache.
     * This is used for unified logging in the Fakt Trace tree.
     *
     * @return Saved FIR time in nanoseconds, or 0 if no cache was loaded
     */
    fun getSavedFirTimeNanos(): Long = savedFirTimeNanos

    /**
     * Write metadata storage to cache file.
     *
     * Called after FIR phase completes in producer mode.
     *
     * @param storage FirMetadataStorage containing validated metadata
     * @return CacheWriteResult with timing info, or null if nothing was cached
     */
    fun writeCache(storage: FirMetadataStorage): CacheWriteResult? {
        if (!isProducerMode) return null

        val outputPath = metadataOutputPath ?: return null
        val interfaces = storage.getAllInterfaces()
        val classes = storage.getAllClasses()

        if (interfaces.isEmpty() && classes.isEmpty()) {
            return null // Nothing to cache
        }

        val startTime = System.nanoTime()

        val serializableInterfaces = interfaces.map { MetadataCacheSerializer.toSerializable(it) }
        val serializableClasses = classes.map { MetadataCacheSerializer.toSerializable(it) }

        // Compute combined signature
        val signatures =
            serializableInterfaces.map { it.sourceFileSignature } +
                serializableClasses.map { it.sourceFileSignature }
        val cacheSignature = MetadataCacheSerializer.computeCombinedSignature(signatures)

        val cache =
            FirMetadataCache(
                cacheSignature = cacheSignature,
                interfaces = serializableInterfaces,
                classes = serializableClasses,
            )

        MetadataCacheSerializer.serialize(cache, outputPath)

        val durationNanos = System.nanoTime() - startTime

        val result =
            CacheWriteResult(
                interfaceCount = interfaces.size,
                classCount = classes.size,
                durationNanos = durationNanos,
                outputPath = outputPath,
            )
        lastWriteResult = result
        return result
    }

    /**
     * Get the last cache write result.
     *
     * Used by IR phase to log a summary once at the end.
     */
    fun getLastWriteResult(): CacheWriteResult? = lastWriteResult

    /**
     * Validate cache against current source files.
     *
     * Checks:
     * 1. Cache version matches current version
     * 2. All source file signatures still match (files not modified)
     *
     * @param cache Deserialized cache to validate
     * @return true if cache is valid and can be used
     */
    private fun validateCache(cache: FirMetadataCache): Boolean {
        // Version check
        if (cache.version != FirMetadataCache.CURRENT_VERSION) {
            return false
        }

        // Validate interface source file signatures
        for (iface in cache.interfaces) {
            val currentSignature =
                MetadataCacheSerializer.computeFileSignature(iface.sourceFilePath)
            if (currentSignature != iface.sourceFileSignature) {
                return false
            }
        }

        // Validate class source file signatures
        for (clazz in cache.classes) {
            val currentSignature =
                MetadataCacheSerializer.computeFileSignature(clazz.sourceFilePath)
            if (currentSignature != clazz.sourceFileSignature) {
                return false
            }
        }

        return true
    }
}

/**
 * Result of writing the FIR metadata cache.
 *
 * @property interfaceCount Number of interfaces written to cache
 * @property classCount Number of classes written to cache
 * @property durationNanos Time spent writing the cache in nanoseconds
 * @property outputPath Path where the cache was written
 */
data class CacheWriteResult(
    val interfaceCount: Int,
    val classCount: Int,
    val durationNanos: Long,
    val outputPath: String,
)
