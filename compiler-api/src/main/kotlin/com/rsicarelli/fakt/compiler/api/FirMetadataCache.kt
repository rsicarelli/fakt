// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.api

import kotlinx.serialization.Serializable

/**
 * Serializable cache for FIR metadata enabling cross-compilation caching in KMP projects.
 *
 * ## Purpose
 *
 * In Kotlin Multiplatform projects, identical FIR analysis runs N times (once per platform).
 * This cache stores the analysis results from metadata compilation so platform compilations
 * can skip redundant FIR analysis.
 *
 * ## Data Flow
 *
 * ```
 * compileCommonMainKotlinMetadata (PRODUCER)
 * ├─ FIR: Analyzes @Fake interfaces
 * ├─ Serializes ValidatedFakeInterface → JSON cache file
 * └─ Output: build/generated/fakt/metadata/fakt-cache.json
 *
 * compileKotlinJvm/IosX64/Js (CONSUMERS)
 * ├─ FIR: Detects cache file exists
 * ├─ Loads cached metadata → FirMetadataStorage
 * └─ SKIPS FIR analysis
 * ```
 *
 * @property version Cache format version for backward compatibility
 * @property cacheSignature Combined signature of all cached interfaces (for invalidation)
 * @property interfaces Cached interface metadata
 * @property classes Cached class metadata
 * @property generatedAt Timestamp for debugging/logging
 */
@Serializable
data class FirMetadataCache(
    val version: Int = CURRENT_VERSION,
    val cacheSignature: String,
    val interfaces: List<SerializableFakeInterface>,
    val classes: List<SerializableFakeClass>,
    val generatedAt: Long = System.currentTimeMillis(),
) {
    /**
     * Total FIR validation time from all cached interfaces and classes.
     * Used to report how much time was saved by using the cache.
     */
    val totalFirTimeNanos: Long
        get() = interfaces.sumOf { it.validationTimeNanos } + classes.sumOf { it.validationTimeNanos }

    /**
     * Total number of fakes (interfaces + classes) in the cache.
     */
    val totalFakesCount: Int
        get() = interfaces.size + classes.size

    /**
     * Returns a formatted cache load summary message.
     *
     * @param timeFormatter Function to format nanoseconds to human-readable string
     * @return Summary message like "FIR cache loaded (saved 234ms FIR time from 122 fakes)"
     */
    fun formatLoadSummary(timeFormatter: (Long) -> String): String {
        val savedTime = timeFormatter(totalFirTimeNanos)
        return "FIR cache loaded (saved $savedTime FIR time from $totalFakesCount fakes)"
    }

    companion object {
        /**
         * Current cache format version.
         * Increment this when making breaking changes to the cache format.
         */
        const val CURRENT_VERSION: Int = 1
    }
}

/**
 * Serializable version of ValidatedFakeInterface.
 *
 * Excludes ClassId (not serializable) - uses string representation instead.
 * The ClassId string format is "package.name/SimpleName" or "package.name/Outer.Inner" for nested.
 *
 * @property classIdString ClassId as string (e.g., "com.example/UserService")
 * @property simpleName Simple class name (e.g., "UserService")
 * @property packageName Package name (e.g., "com.example")
 * @property typeParameters Class-level type parameters with bounds
 * @property properties Properties declared directly in this interface
 * @property functions Functions declared directly in this interface
 * @property inheritedProperties Properties inherited from super-interfaces
 * @property inheritedFunctions Functions inherited from super-interfaces
 * @property sourceFilePath Path to source file (for cache invalidation)
 * @property sourceFileSignature MD5 hash of source file (for cache invalidation)
 * @property validationTimeNanos Time spent validating this interface in FIR phase
 */
@Serializable
data class SerializableFakeInterface(
    val classIdString: String,
    val simpleName: String,
    val packageName: String,
    val typeParameters: List<SerializableTypeParameterInfo>,
    val properties: List<SerializablePropertyInfo>,
    val functions: List<SerializableFunctionInfo>,
    val inheritedProperties: List<SerializablePropertyInfo>,
    val inheritedFunctions: List<SerializableFunctionInfo>,
    val sourceFilePath: String,
    val sourceFileSignature: String,
    val validationTimeNanos: Long,
)

/**
 * Serializable version of ValidatedFakeClass.
 *
 * Similar to [SerializableFakeInterface] but for abstract/open classes.
 *
 * @property classIdString ClassId as string
 * @property simpleName Simple class name
 * @property packageName Package name
 * @property typeParameters Class-level type parameters with bounds
 * @property abstractProperties Abstract properties requiring implementation
 * @property openProperties Open properties that can be overridden
 * @property abstractMethods Abstract methods requiring implementation
 * @property openMethods Open methods that can be overridden
 * @property sourceFilePath Path to source file
 * @property sourceFileSignature MD5 hash of source file
 * @property validationTimeNanos Time spent validating this class in FIR phase
 */
@Serializable
data class SerializableFakeClass(
    val classIdString: String,
    val simpleName: String,
    val packageName: String,
    val typeParameters: List<SerializableTypeParameterInfo>,
    val abstractProperties: List<SerializablePropertyInfo>,
    val openProperties: List<SerializablePropertyInfo>,
    val abstractMethods: List<SerializableFunctionInfo>,
    val openMethods: List<SerializableFunctionInfo>,
    val sourceFilePath: String,
    val sourceFileSignature: String,
    val validationTimeNanos: Long,
)

/**
 * Serializable type parameter information.
 *
 * Examples:
 * - Simple: `T` → SerializableTypeParameterInfo("T", emptyList())
 * - Bounded: `T : Comparable<T>` → SerializableTypeParameterInfo("T", listOf("Comparable<T>"))
 * - Multiple bounds: `T : Comparable<T>, Serializable` →
 *     SerializableTypeParameterInfo("T", listOf("Comparable<T>", "Serializable"))
 *
 * @property name Type parameter name (e.g., "T", "K", "V")
 * @property bounds Upper bounds/constraints as strings
 */
@Serializable
data class SerializableTypeParameterInfo(
    val name: String,
    val bounds: List<String>,
)

/**
 * Serializable property information.
 *
 * @property name Property name
 * @property type Type as string (e.g., "String", "List<T>", "Map<K, V>")
 * @property isMutable True if `var`, false if `val`
 * @property isNullable True if type is nullable (e.g., `String?`)
 */
@Serializable
data class SerializablePropertyInfo(
    val name: String,
    val type: String,
    val isMutable: Boolean,
    val isNullable: Boolean,
)

/**
 * Serializable function information.
 *
 * @property name Function name
 * @property parameters Function parameters
 * @property returnType Return type as string
 * @property isSuspend True if `suspend fun`
 * @property isInline True if `inline fun`
 * @property typeParameters Method-level type parameters
 * @property typeParameterBounds Map of type parameter name to its bound (e.g., "R" → "TValue")
 */
@Serializable
data class SerializableFunctionInfo(
    val name: String,
    val parameters: List<SerializableParameterInfo>,
    val returnType: String,
    val isSuspend: Boolean,
    val isInline: Boolean,
    val typeParameters: List<SerializableTypeParameterInfo>,
    val typeParameterBounds: Map<String, String>,
)

/**
 * Serializable function parameter information.
 *
 * @property name Parameter name
 * @property type Parameter type as string
 * @property hasDefaultValue True if parameter has default value
 * @property defaultValueCode Rendered default value expression (e.g., "null", "\"GET\"", "30000L")
 * @property isVararg True if parameter is vararg
 */
@Serializable
data class SerializableParameterInfo(
    val name: String,
    val type: String,
    val hasDefaultValue: Boolean,
    val defaultValueCode: String?,
    val isVararg: Boolean,
)
