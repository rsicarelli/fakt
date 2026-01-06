// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests KMP source set hierarchy: nativeTest can access fakes from commonMain.
 *
 * Hierarchy: commonMain → nativeMain
 *
 * This test validates that:
 * 1. nativeTest can use FakeCommonPlatformServiceImpl (from commonMain)
 * 2. nativeTest can use FakeNativeOnlyServiceImpl (from nativeMain)
 * 3. Fakes from "parent" source sets are accessible in "child" tests
 * 4. nativeTest is shared across ALL native targets (iOS, macOS, Linux, Windows)
 */
class NativeHierarchyTest {
    @Test
    fun `GIVEN nativeTest WHEN using CommonPlatformService fake THEN should access fake from commonMain`() {
        // Given - Using fake from commonMain (parent source set)
        val commonFake = fakeCommonPlatformService {
            getPlatformName { "Native via Common" }
            log { level, message -> println("[$level] $message") }
        }

        // When
        commonFake.log("INFO", "Test from Native")
        val result = commonFake.getPlatformName()

        // Then
        assertEquals("Native via Common", result)
        assertEquals(1, commonFake.logCallCount.value)
        assertEquals(1, commonFake.getPlatformNameCallCount.value)
    }

    @Test
    fun `GIVEN nativeTest WHEN using NativeOnlyService fake THEN should access fake from nativeMain`() {
        // Given - Using fake from nativeMain (own source set)
        val nativeFake = fakeNativeOnlyService {
            architecture { "x64" }
            callCFunction { name ->
                when (name) {
                    "getpid" -> 1234
                    "getuid" -> 501
                    else -> -1
                }
            }
        }

        // When
        val arch = nativeFake.architecture
        val pid = nativeFake.callCFunction("getpid")
        val uid = nativeFake.callCFunction("getuid")

        // Then
        assertEquals("x64", arch)
        assertEquals(1234, pid)
        assertEquals(501, uid)
        assertEquals(1, nativeFake.architectureCallCount.value)
        assertEquals(2, nativeFake.callCFunctionCallCount.value)
    }

    @Test
    fun `GIVEN nativeTest WHEN using both common and native fakes THEN should access both in hierarchy`() {
        // Given - Using fakes from both commonMain and nativeMain
        val commonFake = fakeCommonPlatformService {
            getPlatformName { "Native" }
            isDebugEnabled { true }
        }
        val nativeFake = fakeNativeOnlyService {
            architecture { "arm64" }
            allocateNativeMemory { size -> size * 8 }
        }

        // When
        val platform = commonFake.getPlatformName()
        val debug = commonFake.isDebugEnabled
        val arch = nativeFake.architecture
        val pointer = nativeFake.allocateNativeMemory(1024)

        // Then - Both fakes work correctly
        assertEquals("Native", platform)
        assertEquals(true, debug)
        assertEquals("arm64", arch)
        assertEquals(8192L, pointer) // 1024 * 8
        assertEquals(1, commonFake.getPlatformNameCallCount.value)
        assertEquals(1, commonFake.isDebugEnabledCallCount.value)
        assertEquals(1, nativeFake.architectureCallCount.value)
        assertEquals(1, nativeFake.allocateNativeMemoryCallCount.value)
    }
}
