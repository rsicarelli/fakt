// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for NativeOnlyService fake generated in nativeTest.
 *
 * This test validates that:
 * 1. Fakt generates fakes for nativeMain interfaces in nativeTest
 * 2. The fake is shared across ALL native targets (iOS, macOS, Linux, Windows)
 * 3. The fake is NOT available in JVM, JS, or WASM tests
 * 4. Native-specific operations can be mocked
 */
class NativeOnlyServiceTest {
    @Test
    fun `GIVEN NativeOnlyService fake WHEN allocating memory THEN should return pointer`() {
        // Given
        var nextPointer = 0x1000L
        val fake = fakeNativeOnlyService {
            allocateNativeMemory { size ->
                val pointer = nextPointer
                nextPointer += size
                pointer
            }
        }

        // When
        val pointer1 = fake.allocateNativeMemory(1024)
        val pointer2 = fake.allocateNativeMemory(2048)

        // Then
        assertEquals(0x1000L, pointer1)
        assertEquals(0x1400L, pointer2) // 0x1000 + 1024
        assertEquals(2, fake.allocateNativeMemoryCallCount.value)
    }

    @Test
    fun `GIVEN NativeOnlyService fake WHEN freeing memory THEN should track calls`() {
        // Given
        val fake = fakeNativeOnlyService {
            freeNativeMemory { /* no-op */ }
        }

        // When
        fake.freeNativeMemory(0x1000L)
        fake.freeNativeMemory(0x2000L)

        // Then
        assertEquals(2, fake.freeNativeMemoryCallCount.value)
    }

    @Test
    fun `GIVEN NativeOnlyService fake WHEN calling C function THEN should return result`() {
        // Given
        val fake = fakeNativeOnlyService {
            callCFunction { name ->
                when (name) {
                    "getpid" -> 12345
                    "getuid" -> 1000
                    else -> -1
                }
            }
        }

        // When
        val pid = fake.callCFunction("getpid")
        val uid = fake.callCFunction("getuid")
        val unknown = fake.callCFunction("unknown")

        // Then
        assertEquals(12345, pid)
        assertEquals(1000, uid)
        assertEquals(-1, unknown)
        assertEquals(3, fake.callCFunctionCallCount.value)
    }

    @Test
    fun `GIVEN NativeOnlyService fake WHEN accessing architecture THEN should return configured value`() {
        // Given
        val fake = fakeNativeOnlyService {
            architecture { "arm64" }
        }

        // When
        val result = fake.architecture

        // Then
        assertEquals("arm64", result)
        assertEquals(1, fake.architectureCallCount.value)
    }

    @Test
    fun `GIVEN NativeOnlyService fake WHEN using defaults THEN should have sensible defaults`() {
        // Given
        val fake = fakeNativeOnlyService()

        // When
        val pointer = fake.allocateNativeMemory(1024)
        fake.freeNativeMemory(0x1000L)
        val result = fake.callCFunction("test")
        val arch = fake.architecture

        // Then - Fakt uses identity function { it } for single-param methods
        assertEquals(1024L, pointer) // Default: identity returns input (1024L)
        assertEquals(0, result) // Default: 0 (callCFunction has String param, result is Int)
        assertEquals("", arch) // Default: empty string (property with no params)
        assertEquals(1, fake.allocateNativeMemoryCallCount.value)
        assertEquals(1, fake.freeNativeMemoryCallCount.value)
        assertEquals(1, fake.callCFunctionCallCount.value)
        assertEquals(1, fake.architectureCallCount.value)
    }
}
