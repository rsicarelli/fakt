// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.defaultParameters

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for default parameter support in Fakt-generated fakes.
 *
 * These tests validate that:
 * 1. Default parameters from interfaces are preserved in generated fakes
 * 2. Functions can be called without providing optional parameters
 * 3. Default values match the interface specification
 * 4. All platforms (JVM, JS, Native, Wasm) handle defaults correctly
 *
 * ## Testing Strategy
 * Each test follows GIVEN-WHEN-THEN pattern:
 * - GIVEN: Configure fake behavior
 * - WHEN: Call function with and without optional parameters
 * - THEN: Verify correct behavior using default values
 */
class DefaultParametersTest {
    @Test
    fun `GIVEN single default parameter WHEN calling without optional param THEN uses default value`() {
        // GIVEN - Fake configured to format amount and currency
        val service =
            fakeDefaultParametersService {
                singleDefault { amount, currency ->
                    "Payment: $amount $currency"
                }
            }

        // WHEN - Call without currency (should use "USD" default)
        val resultWithDefault = service.singleDefault(100.0)

        // THEN - Default "USD" is used
        assertEquals("Payment: 100.0 USD", resultWithDefault)

        // WHEN - Call with explicit currency
        val resultWithExplicit = service.singleDefault(100.0, "EUR")

        // THEN - Explicit value is used
        assertEquals("Payment: 100.0 EUR", resultWithExplicit)
    }

    @Test
    fun `GIVEN all parameters have defaults WHEN calling without any params THEN uses all defaults`() {
        // GIVEN - Fake configured to format configuration
        val service =
            fakeDefaultParametersService {
                allDefaults { timeout, retries, enabled ->
                    "Config: timeout=$timeout, retries=$retries, enabled=$enabled"
                }
            }

        // WHEN - Call without any parameters (all defaults)
        val resultAllDefaults = service.allDefaults()

        // THEN - All defaults are used (5000L, 3, true)
        assertEquals("Config: timeout=5000, retries=3, enabled=true", resultAllDefaults)

        // WHEN - Call with some parameters
        val resultPartial = service.allDefaults(timeout = 10000L)

        // THEN - Specified value used, others default
        assertEquals("Config: timeout=10000, retries=3, enabled=true", resultPartial)

        // WHEN - Call with all parameters
        val resultAll = service.allDefaults(timeout = 1000L, retries = 5, enabled = false)

        // THEN - All specified values used
        assertEquals("Config: timeout=1000, retries=5, enabled=false", resultAll)
    }

    @Test
    fun `GIVEN mixed required and optional params WHEN calling with only required THEN defaults used`() {
        // GIVEN - Fake configured to combine parameters
        val service =
            fakeDefaultParametersService {
                mixedDefaults { required, optional, another ->
                    "$required|$optional|$another"
                }
            }

        // WHEN - Call with only required parameter
        val resultMinimal = service.mixedDefaults("test")

        // THEN - Defaults are used (42, "default")
        assertEquals("test|42|default", resultMinimal)

        // WHEN - Call with required + one optional
        val resultPartial = service.mixedDefaults("test", 99)

        // THEN - Specified optional used, other default
        assertEquals("test|99|default", resultPartial)

        // WHEN - Call with all parameters
        val resultAll = service.mixedDefaults("test", 99, "custom")

        // THEN - All specified values used
        assertEquals("test|99|custom", resultAll)
    }

    @Test
    fun `GIVEN nullable default WHEN calling without optional param THEN uses null default`() {
        // GIVEN - Fake configured to handle nullable metadata
        val service =
            fakeDefaultParametersService {
                nullableDefault { id, metadata ->
                    val metaStr = metadata?.entries?.joinToString() ?: "no-metadata"
                    Result.success("$id:$metaStr")
                }
            }

        // WHEN - Call without metadata (should use null default)
        val resultWithDefault = service.nullableDefault("user-123")

        // THEN - Null default is used
        assertTrue(resultWithDefault.isSuccess)
        assertEquals("user-123:no-metadata", resultWithDefault.getOrNull())

        // WHEN - Call with explicit metadata
        val resultWithMetadata = service.nullableDefault("user-123", mapOf("role" to "admin"))

        // THEN - Provided metadata is used
        assertTrue(resultWithMetadata.isSuccess)
        assertEquals("user-123:role=admin", resultWithMetadata.getOrNull())
    }

    @Test
    fun `GIVEN suspend function with defaults WHEN calling without optional param THEN uses default`() =
        runTest {
            // GIVEN - Fake configured for async operation
            val service =
                fakeDefaultParametersService {
                    suspendWithDefaults { url, timeout ->
                        Result.success("Fetched $url with timeout $timeout")
                    }
                }

            // WHEN - Call without timeout (should use 3000L default)
            val resultWithDefault = service.suspendWithDefaults("https://api.example.com")

            // THEN - Default timeout is used
            assertTrue(resultWithDefault.isSuccess)
            assertEquals("Fetched https://api.example.com with timeout 3000", resultWithDefault.getOrNull())

            // WHEN - Call with explicit timeout
            val resultWithTimeout = service.suspendWithDefaults("https://api.example.com", 5000L)

            // THEN - Explicit timeout is used
            assertTrue(resultWithTimeout.isSuccess)
            assertEquals("Fetched https://api.example.com with timeout 5000", resultWithTimeout.getOrNull())
        }

    @Test
    fun `GIVEN complex collection defaults WHEN calling without params THEN uses empty collections`() {
        // GIVEN - Fake configured to count items and config entries
        val service =
            fakeDefaultParametersService {
                complexDefaults { items, config ->
                    items.size + config.size
                }
            }

        // WHEN - Call without any parameters (should use emptyList() and emptyMap())
        val resultEmpty = service.complexDefaults()

        // THEN - Empty defaults are used (0 items + 0 config = 0)
        assertEquals(0, resultEmpty)

        // WHEN - Call with items only
        val resultWithItems = service.complexDefaults(items = listOf("a", "b", "c"))

        // THEN - Provided items used, config defaults to empty
        assertEquals(3, resultWithItems)

        // WHEN - Call with config only
        val resultWithConfig = service.complexDefaults(config = mapOf("key1" to "val1", "key2" to "val2"))

        // THEN - Provided config used, items defaults to empty
        assertEquals(2, resultWithConfig)

        // WHEN - Call with both parameters
        val resultBoth =
            service.complexDefaults(
                items = listOf("a", "b"),
                config = mapOf("k" to "v"),
            )

        // THEN - Both provided values used
        assertEquals(3, resultBoth)
    }

    @Test
    fun `GIVEN multiple primitive defaults WHEN calling without params THEN uses all primitive defaults`() {
        // GIVEN - Fake configured to format all parameters
        val service =
            fakeDefaultParametersService {
                primitiveDefaults { name, count, rate, active ->
                    "name=$name,count=$count,rate=$rate,active=$active"
                }
            }

        // WHEN - Call without any parameters
        val resultAllDefaults = service.primitiveDefaults()

        // THEN - All primitive defaults used ("", 0, 0.0, false)
        assertEquals("name=,count=0,rate=0.0,active=false", resultAllDefaults)

        // WHEN - Call with custom values
        val resultCustom = service.primitiveDefaults("test", 5, 3.14, true)

        // THEN - Custom values used
        assertEquals("name=test,count=5,rate=3.14,active=true", resultCustom)
    }

    @Test
    fun `GIVEN default in middle position WHEN calling with first and last THEN middle uses default`() {
        // GIVEN - Fake configured to combine parameters
        val service =
            fakeDefaultParametersService {
                defaultInMiddle { first, middle, last ->
                    "$first-$middle-$last"
                }
            }

        // WHEN - Call with first and last (named parameters required)
        val resultWithDefault = service.defaultInMiddle(first = "start", last = "end")

        // THEN - Middle uses default value (10)
        assertEquals("start-10-end", resultWithDefault)

        // WHEN - Call with all parameters
        val resultAll = service.defaultInMiddle("start", 99, "end")

        // THEN - All specified values used
        assertEquals("start-99-end", resultAll)
    }

    @Test
    fun `GIVEN fake with defaults WHEN checking call tracking THEN tracks calls correctly`() {
        // GIVEN - Fake with default parameters
        val service =
            fakeDefaultParametersService {
                singleDefault { amount, currency ->
                    "$amount $currency"
                }
            }

        // WHEN - Call multiple times with and without defaults
        service.singleDefault(100.0) // Uses default
        service.singleDefault(200.0, "EUR") // Explicit value

        // THEN - Both calls are tracked
        assertEquals(2, service.singleDefaultCallCount.value)
    }

    @Test
    fun `GIVEN unconfigured fake WHEN calling with defaults THEN uses type-safe default behavior`() {
        // GIVEN - Unconfigured fake (no behavior set)
        val service = fakeDefaultParametersService {}

        // WHEN - Call function with defaults
        val result = service.singleDefault(100.0)

        // THEN - Type-safe default behavior is used (empty string for String return)
        assertEquals("", result)
    }
}
