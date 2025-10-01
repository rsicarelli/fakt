// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance

import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance cache for type analysis operations in KtFakes.
 *
 * Optimizes the most expensive O(n) operations identified:
 * - IR type to Kotlin string conversion
 * - Generic type parameter analysis
 * - Default value generation
 * - Function type resolution
 *
 * Thread-safe and designed for concurrent compilation scenarios.
 */
class TypeAnalysisCache {
    // Core caching maps - thread-safe for concurrent compilation
    private val typeStringCache = ConcurrentHashMap<String, String>()
    private val defaultValueCache = ConcurrentHashMap<String, String>()
    private val genericAnalysisCache = ConcurrentHashMap<String, GenericTypeInfo>()
    private val functionTypeCache = ConcurrentHashMap<String, String>()

    // Performance metrics
    private var totalRequests = 0L
    private var cacheHits = 0L

    /**
     * Get cached type string conversion with fallback to computation.
     */
    fun getCachedTypeString(
        typeKey: String,
        preserveTypeParameters: Boolean = true,
        computeFunction: () -> String
    ): String {
        totalRequests++
        val cacheKey = "${typeKey}_preserve_${preserveTypeParameters}"

        return typeStringCache.getOrPut(cacheKey) {
            computeFunction()
        }.also {
            if (typeStringCache.containsKey(cacheKey)) {
                cacheHits++
            }
        }
    }

    /**
     * Get cached default value with fallback to computation.
     */
    fun getCachedDefaultValue(
        typeKey: String,
        computeFunction: () -> String
    ): String {
        totalRequests++

        return defaultValueCache.getOrPut(typeKey) {
            computeFunction()
        }.also {
            if (defaultValueCache.containsKey(typeKey)) {
                cacheHits++
            }
        }
    }

    /**
     * Get cached generic type analysis with fallback to computation.
     */
    fun getCachedGenericAnalysis(
        typeKey: String,
        computeFunction: () -> GenericTypeInfo
    ): GenericTypeInfo {
        totalRequests++

        return genericAnalysisCache.getOrPut(typeKey) {
            computeFunction()
        }.also {
            if (genericAnalysisCache.containsKey(typeKey)) {
                cacheHits++
            }
        }
    }

    /**
     * Get cached function type conversion with fallback to computation.
     */
    fun getCachedFunctionType(
        typeKey: String,
        computeFunction: () -> String
    ): String {
        totalRequests++

        return functionTypeCache.getOrPut(typeKey) {
            computeFunction()
        }.also {
            if (functionTypeCache.containsKey(typeKey)) {
                cacheHits++
            }
        }
    }

    /**
     * Get cache statistics for performance reporting.
     */
    fun getCacheStats(): CacheStats {
        val hitRate = if (totalRequests > 0) {
            (cacheHits.toDouble() / totalRequests.toDouble() * 100).toInt()
        } else 0

        return CacheStats(
            totalRequests = totalRequests,
            cacheHits = cacheHits,
            hitRatePercent = hitRate,
            typeStringCacheSize = typeStringCache.size,
            defaultValueCacheSize = defaultValueCache.size,
            genericAnalysisCacheSize = genericAnalysisCache.size,
            functionTypeCacheSize = functionTypeCache.size
        )
    }

    /**
     * Clear all caches (useful for testing or memory management).
     */
    fun clearAll() {
        typeStringCache.clear()
        defaultValueCache.clear()
        genericAnalysisCache.clear()
        functionTypeCache.clear()
        totalRequests = 0L
        cacheHits = 0L
    }

    /**
     * Pre-warm cache with common types to improve initial performance.
     */
    fun preWarmCache() {
        // Common primitive types
        val commonTypes = listOf(
            "kotlin.String" to "String",
            "kotlin.Int" to "Int",
            "kotlin.Boolean" to "Boolean",
            "kotlin.Unit" to "Unit",
            "kotlin.Long" to "Long",
            "kotlin.Double" to "Double",
            "kotlin.Float" to "Float"
        )

        commonTypes.forEach { (key, value) ->
            typeStringCache[key] = value
        }

        // Common default values
        val commonDefaults = listOf(
            "kotlin.String" to "\"\"",
            "kotlin.Int" to "0",
            "kotlin.Boolean" to "false",
            "kotlin.Unit" to "Unit",
            "kotlin.collections.List" to "emptyList()",
            "kotlin.collections.Map" to "emptyMap()",
            "kotlin.collections.Set" to "emptySet()"
        )

        commonDefaults.forEach { (key, value) ->
            defaultValueCache[key] = value
        }
    }

    /**
     * Get cache efficiency report for performance monitoring.
     */
    fun getEfficiencyReport(): String {
        val stats = getCacheStats()
        return buildString {
            appendLine("Type Analysis Cache Report:")
            appendLine("  • Total requests: ${stats.totalRequests}")
            appendLine("  • Cache hits: ${stats.cacheHits}")
            appendLine("  • Hit rate: ${stats.hitRatePercent}%")
            appendLine("  • Cache sizes:")
            appendLine("    - Type strings: ${stats.typeStringCacheSize}")
            appendLine("    - Default values: ${stats.defaultValueCacheSize}")
            appendLine("    - Generic analysis: ${stats.genericAnalysisCacheSize}")
            appendLine("    - Function types: ${stats.functionTypeCacheSize}")

            when {
                stats.hitRatePercent >= 80 -> appendLine("  ✅ Excellent cache performance!")
                stats.hitRatePercent >= 60 -> appendLine("  ⚡ Good cache performance")
                stats.hitRatePercent >= 40 -> appendLine("  ⚠️ Cache performance could be improved")
                else -> appendLine("  🚨 Poor cache performance - check cache strategy")
            }
        }
    }
}

/**
 * Generic type information for caching complex type analysis.
 */
data class GenericTypeInfo(
    val hasClassLevelGenerics: Boolean,
    val hasMethodLevelGenerics: Boolean,
    val typeParameterNames: List<String>,
    val complexityScore: Int // Higher = more complex generic handling needed
)

/**
 * Cache performance statistics.
 */
data class CacheStats(
    val totalRequests: Long,
    val cacheHits: Long,
    val hitRatePercent: Int,
    val typeStringCacheSize: Int,
    val defaultValueCacheSize: Int,
    val genericAnalysisCacheSize: Int,
    val functionTypeCacheSize: Int
)