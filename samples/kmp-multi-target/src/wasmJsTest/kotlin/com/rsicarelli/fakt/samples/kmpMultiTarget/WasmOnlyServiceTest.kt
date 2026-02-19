// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for WasmOnlyService fake generated in wasmJsTest.
 *
 * This test validates that:
 * 1. Fakt generates fakes for wasmJsMain interfaces in wasmJsTest
 * 2. The fake is ONLY available in WASM tests (not in other platforms)
 * 3. WASM-specific operations can be mocked
 */
class WasmOnlyServiceTest {
    @Test
    fun `GIVEN WasmOnlyService fake WHEN allocating memory THEN should return pointer`() {
        // Given
        var nextPointer = 1024
        val fake = fakeWasmOnlyService {
            allocateMemory { size ->
                val pointer = nextPointer
                nextPointer += size
                pointer
            }
        }

        // When
        val pointer1 = fake.allocateMemory(256)
        val pointer2 = fake.allocateMemory(512)

        // Then
        assertEquals(1024, pointer1)
        assertEquals(1280, pointer2) // 1024 + 256
        assertEquals(2, fake.allocateMemoryCalls.value.size)
    }

    @Test
    fun `GIVEN WasmOnlyService fake WHEN freeing memory THEN should return success`() {
        // Given
        val allocatedPointers = mutableSetOf(1024, 2048)
        val fake = fakeWasmOnlyService {
            freeMemory { pointer -> allocatedPointers.remove(pointer) }
        }

        // When
        val freed1 = fake.freeMemory(1024)
        val freed2 = fake.freeMemory(9999) // not allocated

        // Then
        assertTrue(freed1)
        assertFalse(freed2)
        assertEquals(2, fake.freeMemoryCalls.value.size)
    }

    @Test
    fun `GIVEN WasmOnlyService fake WHEN calling WASM function THEN should return result`() {
        // Given
        val fake = fakeWasmOnlyService {
            callWasmFunction { name, args ->
                when (name) {
                    "add" -> args.sum()
                    "multiply" -> args.fold(1) { acc, n -> acc * n }
                    else -> 0
                }
            }
        }

        // When
        val sum = fake.callWasmFunction("add", listOf(1, 2, 3, 4))
        val product = fake.callWasmFunction("multiply", listOf(2, 3, 4))

        // Then
        assertEquals(10, sum) // 1 + 2 + 3 + 4
        assertEquals(24, product) // 2 * 3 * 4
        assertEquals(2, fake.callWasmFunctionCalls.value.size)
    }

    @Test
    fun `GIVEN WasmOnlyService fake WHEN accessing memory pages THEN should return configured value`() {
        // Given
        val fake = fakeWasmOnlyService {
            memoryPages { 16 }
        }

        // When
        val result = fake.memoryPages

        // Then
        assertEquals(16, result)
        assertEquals(1, fake.memoryPagesCalls.value.size)
    }

    @Test
    fun `GIVEN WasmOnlyService fake WHEN using defaults THEN should have sensible defaults`() {
        // Given
        val fake = fakeWasmOnlyService()

        // When
        val pointer = fake.allocateMemory(256)
        val freed = fake.freeMemory(1024)
        val result = fake.callWasmFunction("test", emptyList())
        val pages = fake.memoryPages

        // Then - Fakt uses identity function { it } for single-param methods
        assertEquals(256, pointer) // Default: identity returns input (256)
        assertFalse(freed) // Default: false
        assertEquals(0, result) // Default: 0
        assertEquals(0, pages) // Default: 0
        assertEquals(1, fake.allocateMemoryCalls.value.size)
        assertEquals(1, fake.freeMemoryCalls.value.size)
        assertEquals(1, fake.callWasmFunctionCalls.value.size)
        assertEquals(1, fake.memoryPagesCalls.value.size)
    }
}
