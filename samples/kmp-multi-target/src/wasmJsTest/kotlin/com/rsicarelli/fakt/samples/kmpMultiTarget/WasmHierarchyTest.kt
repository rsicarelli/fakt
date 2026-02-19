// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests KMP source set hierarchy: wasmJsTest can access fakes from commonMain.
 *
 * Hierarchy: commonMain → wasmJsMain
 *
 * This test validates that:
 * 1. wasmJsTest can use FakeCommonPlatformServiceImpl (from commonMain)
 * 2. wasmJsTest can use FakeWasmOnlyServiceImpl (from wasmJsMain)
 * 3. Fakes from "parent" source sets are accessible in "child" tests
 */
class WasmHierarchyTest {
    @Test
    fun `GIVEN wasmJsTest WHEN using CommonPlatformService fake THEN should access fake from commonMain`() {
        // Given - Using fake from commonMain (parent source set)
        val commonFake = fakeCommonPlatformService {
            getPlatformName { "WASM via Common" }
            isDebugEnabled { true }
        }

        // When
        val platform = commonFake.getPlatformName()
        val debug = commonFake.isDebugEnabled

        // Then
        assertEquals("WASM via Common", platform)
        assertEquals(true, debug)
        assertEquals(1, commonFake.getPlatformNameCalls.value.size)
        assertEquals(1, commonFake.isDebugEnabledCalls.value.size)
    }

    @Test
    fun `GIVEN wasmJsTest WHEN using WasmOnlyService fake THEN should access fake from wasmJsMain`() {
        // Given - Using fake from wasmJsMain (own source set)
        val wasmFake = fakeWasmOnlyService {
            memoryPages { 32 }
            callWasmFunction { name, args ->
                when (name) {
                    "add" -> args.sum()
                    else -> 0
                }
            }
        }

        // When
        val pages = wasmFake.memoryPages
        val result = wasmFake.callWasmFunction("add", listOf(10, 20, 30))

        // Then
        assertEquals(32, pages)
        assertEquals(60, result)
        assertEquals(1, wasmFake.memoryPagesCalls.value.size)
        assertEquals(1, wasmFake.callWasmFunctionCalls.value.size)
    }

    @Test
    fun `GIVEN wasmJsTest WHEN using both common and wasm fakes THEN should access both in hierarchy`() {
        // Given - Using fakes from both commonMain and wasmJsMain
        val commonFake = fakeCommonPlatformService {
            log { level, message -> println("[$level] $message") }
            getPlatformName { "WASM" }
        }
        val wasmFake = fakeWasmOnlyService {
            memoryPages { 16 }
            allocateMemory { size -> size * 1024 }
        }

        // When
        commonFake.log("INFO", "Test from WASM")
        val platform = commonFake.getPlatformName()
        val pages = wasmFake.memoryPages
        val pointer = wasmFake.allocateMemory(4)

        // Then - Both fakes work correctly
        assertEquals("WASM", platform)
        assertEquals(16, pages)
        assertEquals(4096, pointer) // 4 * 1024
        assertEquals(1, commonFake.logCalls.value.size)
        assertEquals(1, commonFake.getPlatformNameCalls.value.size)
        assertEquals(1, wasmFake.memoryPagesCalls.value.size)
        assertEquals(1, wasmFake.allocateMemoryCalls.value.size)
    }
}
