// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.performance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains

class TypeAnalysisCacheTest {

    @Test
    fun `GIVEN type analysis cache WHEN caching type strings THEN should improve hit rate`() {
        // Given
        val cache = TypeAnalysisCache()
        val expensiveComputation = { "String" }

        // When - First access (cache miss)
        val result1 = cache.getCachedTypeString("kotlin.String", true, expensiveComputation)

        // When - Second access (cache hit)
        val result2 = cache.getCachedTypeString("kotlin.String", true, expensiveComputation)

        // Then
        assertEquals("String", result1)
        assertEquals("String", result2)

        val stats = cache.getCacheStats()
        assertTrue(stats.cacheHits > 0, "Should have cache hits")
        assertTrue(stats.hitRatePercent > 0, "Should have positive hit rate")
    }

    @Test
    fun `GIVEN type analysis cache WHEN pre-warming cache THEN should have common types cached`() {
        // Given
        val cache = TypeAnalysisCache()

        // When
        cache.preWarmCache()
        val stats = cache.getCacheStats()

        // Then
        assertTrue(stats.typeStringCacheSize > 0, "Should have pre-warmed type strings")
        assertTrue(stats.defaultValueCacheSize > 0, "Should have pre-warmed default values")
    }

    @Test
    fun `GIVEN type analysis cache WHEN caching with different parameters THEN should handle distinct keys`() {
        // Given
        val cache = TypeAnalysisCache()

        // When
        val result1 = cache.getCachedTypeString("kotlin.List", true) { "List<T>" }
        val result2 = cache.getCachedTypeString("kotlin.List", false) { "List<Any>" }

        // Then
        assertEquals("List<T>", result1)
        assertEquals("List<Any>", result2)

        val stats = cache.getCacheStats()
        assertEquals(2, stats.typeStringCacheSize)
    }

    @Test
    fun `GIVEN type analysis cache WHEN generating efficiency report THEN should provide insights`() {
        // Given
        val cache = TypeAnalysisCache()
        cache.preWarmCache()

        // When
        // Generate some cache activity
        cache.getCachedTypeString("kotlin.String", true) { "String" }
        cache.getCachedDefaultValue("kotlin.Int") { "0" }

        val report = cache.getEfficiencyReport()

        // Then
        assertContains(report, "Type Analysis Cache Report:")
        assertContains(report, "Total requests:")
        assertContains(report, "Hit rate:")
        assertContains(report, "Cache sizes:")
    }

    @Test
    fun `GIVEN type analysis cache WHEN clearing cache THEN should reset all metrics`() {
        // Given
        val cache = TypeAnalysisCache()
        cache.getCachedTypeString("test", true) { "result" }

        // When
        cache.clearAll()
        val stats = cache.getCacheStats()

        // Then
        assertEquals(0, stats.totalRequests)
        assertEquals(0, stats.cacheHits)
        assertEquals(0, stats.typeStringCacheSize)
    }

    @Test
    fun `GIVEN type analysis cache WHEN caching generic analysis THEN should handle complex types`() {
        // Given
        val cache = TypeAnalysisCache()
        val complexGenericInfo = GenericTypeInfo(
            hasClassLevelGenerics = true,
            hasMethodLevelGenerics = false,
            typeParameterNames = listOf("T", "R"),
            complexityScore = 3
        )

        // When
        val result = cache.getCachedGenericAnalysis("Repository<T,R>") { complexGenericInfo }

        // Then
        assertEquals(complexGenericInfo, result)
        assertTrue(result.hasClassLevelGenerics)
        assertEquals(2, result.typeParameterNames.size)
        assertEquals(3, result.complexityScore)
    }
}