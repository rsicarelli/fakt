// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.metadata

import org.jetbrains.kotlin.name.ClassId
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe storage for passing validated metadata from FIR phase to IR phase.
 *
 * ## Design Rationale
 *
 * **Why not serialize?**
 * - Same JVM process, no need for serialization overhead
 * - Type-safe at compile time
 *
 * **Why ConcurrentHashMap?**
 * - Kotlin compiler may process files in parallel
 * - FIR checkers may run concurrently
 * - IR generation may be multi-threaded
 *
 * **Why keyed by ClassId?**
 * - Unique identifier across modules
 * - Fast lookup if IR needs specific interface
 * - Matches Kotlin compiler's internal representation
 */
class FirMetadataStorage {
    private val interfaces = ConcurrentHashMap<ClassId, ValidatedFakeInterface>()
    private val classes = ConcurrentHashMap<ClassId, ValidatedFakeClass>()

    /** Count of interfaces loaded from KMP cache (skipped FIR analysis) */
    @Volatile
    var interfaceCacheHits: Int = 0
        private set

    /** Count of classes loaded from KMP cache (skipped FIR analysis) */
    @Volatile
    var classCacheHits: Int = 0
        private set

    /** Total cache hits (interfaces + classes) */
    val totalCacheHits: Int get() = interfaceCacheHits + classCacheHits

    /** Increment interface cache hit counter (called when loading from KMP cache) */
    fun incrementInterfaceCacheHits() {
        interfaceCacheHits++
    }

    /** Increment class cache hit counter (called when loading from KMP cache) */
    fun incrementClassCacheHits() {
        classCacheHits++
    }

    /**
     * Store validated interface metadata from FIR phase.
     *
     * Called by FIR checkers after validating an @Fake interface.
     *
     * @param metadata Validated interface metadata
     * @throws IllegalStateException if interface already stored (duplicate registration)
     */
    fun storeInterface(metadata: ValidatedFakeInterface) {
        val previous = interfaces.putIfAbsent(metadata.classId, metadata)
        if (previous != null) {
            error(
                "Duplicate @Fake interface registration: ${metadata.classId.asFqNameString()}. " +
                    "This is a compiler bug - please report it.",
            )
        }
    }

    /**
     * Store validated class metadata from FIR phase.
     *
     * Called by FIR checkers after validating an @Fake class.
     *
     * @param metadata Validated class metadata
     * @throws IllegalStateException if class already stored (duplicate registration)
     */
    fun storeClass(metadata: ValidatedFakeClass) {
        val previous = classes.putIfAbsent(metadata.classId, metadata)
        if (previous != null) {
            error(
                "Duplicate @Fake class registration: ${metadata.classId.asFqNameString()}. " +
                    "This is a compiler bug - please report it.",
            )
        }
    }

    /**
     * Get all validated interfaces for IR generation.
     *
     * Called by IR generation extension to get list of interfaces to generate fakes for.
     *
     * @return Immutable collection of validated interfaces
     */
    fun getAllInterfaces(): Collection<ValidatedFakeInterface> = interfaces.values.toList()

    /**
     * Get all validated classes for IR generation.
     *
     * Called by IR generation extension to get list of classes to generate fakes for.
     *
     * @return Immutable collection of validated classes
     */
    fun getAllClasses(): Collection<ValidatedFakeClass> = classes.values.toList()

    /**
     * Get specific interface by ClassId (optional - for targeted lookups).
     *
     * @param classId Interface class identifier
     * @return Validated interface metadata, or null if not found
     */
    fun getInterface(classId: ClassId): ValidatedFakeInterface? = interfaces[classId]

    /**
     * Get specific class by ClassId (optional - for targeted lookups).
     *
     * @param classId Class identifier
     * @return Validated class metadata, or null if not found
     */
    fun getClass(classId: ClassId): ValidatedFakeClass? = classes[classId]

    /**
     * Check if any @Fake declarations were found.
     *
     * Useful for early-exit optimization in IR phase.
     *
     * @return true if at least one interface or class registered
     */
    fun isEmpty(): Boolean = interfaces.isEmpty() && classes.isEmpty()

    /**
     * Get total count of registered fakes (for metrics/logging).
     *
     * @return Total number of interfaces + classes
     */
    fun totalCount(): Int = interfaces.size + classes.size

    /**
     * Clear all stored metadata.
     *
     * **WARNING**: Only for testing! Never call in production code.
     * Compilation sessions should create new storage instances.
     */
    internal fun clearForTesting() {
        interfaces.clear()
        classes.clear()
    }

    /**
     * Debug information about stored metadata.
     *
     * @return Human-readable summary
     */
    override fun toString(): String = "FirMetadataStorage(interfaces=${interfaces.size}, classes=${classes.size})"
}
