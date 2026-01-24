// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.annotations

import com.rsicarelli.fakt.Fake

/**
 * Test case: Custom annotation with string argument.
 *
 * The generated FakeFeatureFlagServiceImpl should include:
 * ```kotlin
 * @FeatureFlag(name = "user-preferences", enabled = true)
 * class FakeFeatureFlagServiceImpl : FeatureFlagService { ... }
 * ```
 *
 * Tests:
 * - Custom annotation propagation
 * - String argument
 * - Boolean argument
 * - Named arguments
 */
@Fake
@FeatureFlag(name = "user-preferences", enabled = true)
interface FeatureFlagService {
    fun isFeatureEnabled(): Boolean
    fun getFeatureConfig(): Map<String, String>
}

/**
 * Test case: Custom annotation with disabled flag.
 *
 * Tests boolean false value propagation.
 */
@Fake
@FeatureFlag(name = "experimental-ui", enabled = false)
interface DisabledFeatureService {
    fun getExperimentalUI(): String?
}

/**
 * Test case: Custom annotation with numeric arguments.
 *
 * The generated FakeMetricsServiceImpl should include:
 * ```kotlin
 * @Metrics(priority = 10, version = 2.5, maxRetries = 5L)
 * class FakeMetricsServiceImpl : MetricsService { ... }
 * ```
 *
 * Tests:
 * - Int literal argument
 * - Double literal argument
 * - Long literal argument
 */
@Fake
@Metrics(priority = 10, version = 2.5, maxRetries = 5L)
interface MetricsService {
    fun recordMetric(name: String, value: Double)
    fun getMetricCount(): Long
}

/**
 * Test case: Custom annotation with array argument.
 *
 * The generated FakeTaggedServiceImpl should include:
 * ```kotlin
 * @Tags("api", "v2", "production")
 * class FakeTaggedServiceImpl : TaggedService { ... }
 * ```
 *
 * Tests:
 * - Vararg annotation argument
 * - Multiple string values in array
 */
@Fake
@Tags("api", "v2", "production")
interface TaggedService {
    fun getTags(): List<String>
    fun hasTag(tag: String): Boolean
}

/**
 * Test case: Custom annotation with enum argument.
 *
 * The generated FakeLoggedServiceImpl should include:
 * ```kotlin
 * @LogLevel(level = LogLevel.Level.DEBUG)
 * class FakeLoggedServiceImpl : LoggedService { ... }
 * ```
 *
 * Tests:
 * - Nested enum class argument
 * - Enum constant propagation
 */
@Fake
@LogLevel(level = LogLevel.Level.DEBUG)
interface LoggedService {
    fun log(message: String)
    fun getCurrentLevel(): LogLevel.Level
}

/**
 * Test case: Custom annotation with KClass argument.
 *
 * The generated FakeSerializableServiceImpl should include:
 * ```kotlin
 * @Serializer(with = JsonSerializer::class)
 * class FakeSerializableServiceImpl : SerializableService { ... }
 * ```
 *
 * Tests:
 * - KClass reference argument
 * - Class reference propagation
 */
@Fake
@Serializer(with = JsonSerializer::class)
interface SerializableService {
    fun serialize(data: Any): String
    fun deserialize(json: String): Any
}
