// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)

package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.annotations

import kotlin.native.HiddenFromObjC
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for annotation propagation in generated fakes.
 *
 * These tests verify that:
 * 1. Generated fakes compile with propagated annotations
 * 2. Fakes work functionally as expected
 *
 * Key verification: If annotations aren't propagated correctly, these tests
 * would fail to compile (e.g., @OptIn for experimental APIs would cause errors).
 */
@OptIn(ExperimentalFaktApi::class, BetaFaktApi::class)
class AnnotationPropagationTest {

    // ========================================================================
    // @OptIn Annotation Tests
    // ========================================================================

    @Test
    fun `GIVEN OptInService fake WHEN calling experimental methods THEN should work without opt-in warning`() =
        runTest {
            // Given - The fake has @OptIn(ExperimentalFaktApi::class) propagated
            val fake = fakeOptInService {
                performExperimentalOperation { "experimental result" }
                asyncExperimentalWork { 42 }
            }

            // When
            val syncResult = fake.performExperimentalOperation()
            val asyncResult = fake.asyncExperimentalWork()

            // Then
            assertEquals("experimental result", syncResult)
            assertEquals(42, asyncResult)
        }

    @Test
    fun `GIVEN MultiOptInService fake WHEN using both experimental APIs THEN should compile and work`() {
        // Given - The fake has @OptIn(ExperimentalFaktApi::class, BetaFaktApi::class)
        val fake = fakeMultiOptInService {
            useBothApis { true }
        }

        // When
        val result = fake.useBothApis()

        // Then
        assertTrue(result)
    }

    // ========================================================================
    // @Deprecated Annotation Tests
    // ========================================================================

    @Test
    @Suppress("DEPRECATION")
    fun `GIVEN DeprecatedService fake WHEN calling deprecated methods THEN should work with suppressed warning`() {
        // Given - The fake has @Deprecated annotation propagated
        val fake = fakeDeprecatedService {
            oldMethod { "legacy value" }
        }

        // When
        val result = fake.oldMethod()

        // Then
        assertEquals("legacy value", result)
    }

    @Test
    @Suppress("DEPRECATION")
    fun `GIVEN MinimalDeprecatedService fake WHEN using it THEN should compile with deprecation`() {
        // Given
        val fake = fakeMinimalDeprecatedService {
            legacyOperation { 100 }
        }

        // When
        val result = fake.legacyOperation()

        // Then
        assertEquals(100, result)
    }

    // ========================================================================
    // @Suppress Annotation Tests
    // ========================================================================

    @Test
    fun `GIVEN SuppressSingleService fake WHEN using unchecked cast THEN should not produce warning`() {
        // Given - The fake has @Suppress("UNCHECKED_CAST") propagated
        val fake = fakeSuppressSingleService {
            unsafeCast { value -> @Suppress("UNCHECKED_CAST") (value as Nothing) }
        }

        // When/Then - Just verify it compiles and can be instantiated
        // The actual behavior isn't testable without runtime type checking
    }

    @Test
    @Suppress("DEPRECATION")
    fun `GIVEN SuppressMultipleService fake WHEN using deprecated cast THEN should suppress multiple warnings`() {
        // Given - The fake has @Suppress("UNCHECKED_CAST", "DEPRECATION", "NOTHING_TO_INLINE")
        val fake = fakeSuppressMultipleService {
            legacyCast { value -> @Suppress("UNCHECKED_CAST") (value as Nothing) }
        }

        // When/Then - Verify compilation succeeds
    }

    // ========================================================================
    // Custom Annotation Tests
    // ========================================================================

    @Test
    fun `GIVEN FeatureFlagService fake WHEN checking feature THEN should return configured value`() {
        // Given - The fake has @FeatureFlag(name = "user-preferences", enabled = true)
        val fake = fakeFeatureFlagService {
            isFeatureEnabled { true }
            getFeatureConfig { mapOf("theme" to "dark", "locale" to "en") }
        }

        // When
        val enabled = fake.isFeatureEnabled()
        val config = fake.getFeatureConfig()

        // Then
        assertTrue(enabled)
        assertEquals("dark", config["theme"])
    }

    @Test
    fun `GIVEN DisabledFeatureService fake WHEN checking disabled feature THEN should return null`() {
        // Given - The fake has @FeatureFlag(name = "experimental-ui", enabled = false)
        val fake = fakeDisabledFeatureService {
            getExperimentalUI { null }
        }

        // When
        val result = fake.getExperimentalUI()

        // Then
        assertEquals(null, result)
    }

    @Test
    fun `GIVEN MetricsService fake WHEN recording metrics THEN should accept configured values`() {
        // Given - The fake has @Metrics(priority = 10, version = 2.5, maxRetries = 5L)
        var recordedValue = 0.0
        val fake = fakeMetricsService {
            recordMetric { _, value -> recordedValue = value }
            getMetricCount { 42L }
        }

        // When
        fake.recordMetric("cpu_usage", 75.5)
        val count = fake.getMetricCount()

        // Then
        assertEquals(75.5, recordedValue)
        assertEquals(42L, count)
    }

    @Test
    fun `GIVEN TaggedService fake WHEN getting tags THEN should return configured list`() {
        // Given - The fake has @Tags("api", "v2", "production")
        val fake = fakeTaggedService {
            getTags { listOf("api", "v2", "production") }
            hasTag { tag -> tag in listOf("api", "v2", "production") }
        }

        // When
        val tags = fake.getTags()
        val hasApi = fake.hasTag("api")
        val hasV1 = fake.hasTag("v1")

        // Then
        assertEquals(3, tags.size)
        assertTrue(hasApi)
        assertFalse(hasV1)
    }

    @Test
    fun `GIVEN LoggedService fake WHEN logging THEN should use configured level`() {
        // Given - The fake has @LogLevel(level = LogLevel.Level.DEBUG)
        var loggedMessage = ""
        val fake = fakeLoggedService {
            log { message -> loggedMessage = message }
            getCurrentLevel { LogLevel.Level.DEBUG }
        }

        // When
        fake.log("test message")
        val level = fake.getCurrentLevel()

        // Then
        assertEquals("test message", loggedMessage)
        assertEquals(LogLevel.Level.DEBUG, level)
    }

    @Test
    fun `GIVEN SerializableService fake WHEN serializing THEN should return configured value`() {
        // Given - The fake has @Serializer(with = JsonSerializer::class)
        val fake = fakeSerializableService {
            serialize { data -> """{"data":"$data"}""" }
            deserialize { json -> mapOf("parsed" to json) }
        }

        // When
        val json = fake.serialize("test")
        val parsed = fake.deserialize(json)

        // Then
        assertEquals("""{"data":"test"}""", json)
        assertEquals(mapOf("parsed" to json), parsed)
    }

    // ========================================================================
    // Multiple Annotations Tests
    // ========================================================================

    @Test
    @Suppress("DEPRECATION")
    fun `GIVEN MultiAnnotatedService fake WHEN using it THEN all annotations should be propagated`() {
        // Given - The fake has multiple annotations:
        // @OptIn(ExperimentalFaktApi::class)
        // @Deprecated("Use MultiAnnotatedServiceV2 instead")
        // @Suppress("DEPRECATION")
        // @FeatureFlag(name = "multi-annotated", enabled = true)
        // @Tags("legacy", "monitored")
        val fake = fakeMultiAnnotatedService {
            performLegacyOperation { "legacy result" }
            getStatus { 200 }
        }

        // When
        val operation = fake.performLegacyOperation()
        val status = fake.getStatus()

        // Then
        assertEquals("legacy result", operation)
        assertEquals(200, status)
    }

    @Test
    fun `GIVEN MixedAnnotationService fake WHEN using it THEN should compile with mixed annotations`() {
        // Given - The fake has @OptIn and @Metrics
        val fake = fakeMixedAnnotationService {
            process { true }
        }

        // When
        val result = fake.process()

        // Then
        assertTrue(result)
    }

    @Test
    fun `GIVEN CombinedExperimentalService fake WHEN using all features THEN should work`() {
        // Given - The fake has multiple OptIn markers, Tags, and LogLevel
        val fake = fakeCombinedExperimentalService {
            useAllExperimentalFeatures { "all features enabled" }
        }

        // When
        val result = fake.useAllExperimentalFeatures()

        // Then
        assertEquals("all features enabled", result)
    }

    // ========================================================================
    // Abstract/Open Class Annotation Tests
    // ========================================================================

    @Test
    @Suppress("DEPRECATION")
    fun `GIVEN AnnotatedRepository fake WHEN using abstract class THEN annotations should propagate`() =
        runTest {
            // Given - The abstract class has @OptIn and @Deprecated
            val fake = fakeAnnotatedRepository {
                findById { id -> "found: $id" }
                saveAll { items -> items.size }
            }

            // When
            val found = fake.findById("123")
            val saved = fake.saveAll(listOf("a", "b", "c"))

            // Then
            assertEquals("found: 123", found)
            assertEquals(3, saved)
        }

    @Test
    fun `GIVEN AnnotatedConfig fake WHEN using open class THEN custom annotations should propagate`() {
        // Given - The open class has @FeatureFlag and @Metrics
        val fake = fakeAnnotatedConfig {
            configName { "test-config" }
            getConfigValue { key -> "value-for-$key" }
        }

        // When
        val name = fake.configName
        val value = fake.getConfigValue("api-key")

        // Then
        assertEquals("test-config", name)
        assertEquals("value-for-api-key", value)
    }

    @Test
    fun `GIVEN AnnotatedDataSource fake WHEN using abstract class with generics THEN should work`() =
        runTest {
            // Given - The abstract class has @OptIn, @Suppress, and @Tags
            val fake = fakeAnnotatedDataSource {
                query { sql ->
                    @Suppress("UNCHECKED_CAST")
                    listOf("result1", "result2") as List<Nothing>
                }
                execute { command, params ->
                    @Suppress("UNCHECKED_CAST")
                    "executed: $command" as Nothing
                }
            }

            // When
            val results = fake.query<String>("SELECT * FROM users")

            // Then
            assertEquals(2, results.size)
        }

    // ========================================================================
    // @HiddenFromObjC Annotation Tests
    // ========================================================================

    @Test
    fun `GIVEN HiddenService fake WHEN using hidden interface THEN annotations should propagate`() {
        // Given - The fake has @HiddenFromObjC propagated
        // This annotation hides the class from Objective-C interop
        val fake = fakeHiddenService {
            internalOperation { "internal result" }
            getInternalState { mapOf("key" to "value", "count" to 42) }
        }

        // When
        val operation = fake.internalOperation()
        val state = fake.getInternalState()

        // Then
        assertEquals("internal result", operation)
        assertEquals("value", state["key"])
        assertEquals(42, state["count"])
    }

    @Test
    fun `GIVEN HiddenExperimentalService fake WHEN combining HiddenFromObjC with other annotations THEN should work`() =
        runTest {
            // Given - The fake has @HiddenFromObjC, @OptIn, and @FeatureFlag
            val fake = fakeHiddenExperimentalService {
                performInternalExperiment { true }
                asyncInternalWork { 99 }
            }

            // When
            val experimentResult = fake.performInternalExperiment()
            val asyncResult = fake.asyncInternalWork()

            // Then
            assertTrue(experimentResult)
            assertEquals(99, asyncResult)
        }

    @Test
    fun `GIVEN HiddenRepository fake WHEN using hidden abstract class THEN annotations should propagate`() =
        runTest {
            // Given - The abstract class has @HiddenFromObjC and @Tags
            val fake = fakeHiddenRepository {
                findInternal { key -> if (key == "valid") "found" else null }
                saveInternal { key, value -> key.isNotEmpty() && value.isNotEmpty() }
            }

            // When
            val found = fake.findInternal("valid")
            val notFound = fake.findInternal("invalid")
            val saved = fake.saveInternal("key", "value")

            // Then
            assertEquals("found", found)
            assertNull(notFound)
            assertTrue(saved)
        }
}
