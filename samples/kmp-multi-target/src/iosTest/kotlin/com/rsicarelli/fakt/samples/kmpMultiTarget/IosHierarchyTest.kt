// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests KMP source set hierarchy: iosTest can access fakes from commonMain AND nativeMain.
 *
 * Hierarchy: commonMain → nativeMain → iosMain
 *
 * This test validates that:
 * 1. iosTest can use FakeCommonPlatformServiceImpl (from commonMain - grandparent)
 * 2. iosTest can use FakeNativeOnlyServiceImpl (from nativeMain - parent)
 * 3. iosTest can use FakeIosOnlyServiceImpl (from iosMain - own source set)
 * 4. Fakes from ancestor source sets are accessible in descendant tests
 *
 * CRITICAL: This is the most complex hierarchy test because iOS inherits from TWO levels.
 */
class IosHierarchyTest {
    @Test
    fun `GIVEN iosTest WHEN using CommonPlatformService fake THEN should access fake from commonMain`() {
        // Given - Using fake from commonMain (grandparent source set)
        val commonFake = fakeCommonPlatformService {
            getPlatformName { "iOS via Common" }
            isDebugEnabled { true }
        }

        // When
        val platform = commonFake.getPlatformName()
        val debug = commonFake.isDebugEnabled

        // Then
        assertEquals("iOS via Common", platform)
        assertEquals(true, debug)
        assertEquals(1, commonFake.getPlatformNameCallCount)
        assertEquals(1, commonFake.isDebugEnabledCallCount)
    }

    @Test
    fun `GIVEN iosTest WHEN using NativeOnlyService fake THEN should access fake from nativeMain`() {
        // Given - Using fake from nativeMain (parent source set)
        val nativeFake = fakeNativeOnlyService {
            architecture { "arm64" }
            callCFunction { name ->
                when (name) {
                    "getpid" -> 42
                    else -> -1
                }
            }
        }

        // When
        val arch = nativeFake.architecture
        val pid = nativeFake.callCFunction("getpid")

        // Then
        assertEquals("arm64", arch)
        assertEquals(42, pid)
        assertEquals(1, nativeFake.architectureCallCount)
        assertEquals(1, nativeFake.callCFunctionCallCount)
    }

    @Test
    fun `GIVEN iosTest WHEN using IosOnlyService fake THEN should access fake from iosMain`() {
        // Given - Using fake from iosMain (own source set)
        val iosFake = fakeIosOnlyService {
            getDeviceModel { "iPhone 15 Pro" }
            isSimulator { true }
        }

        // When
        val model = iosFake.getDeviceModel()
        val simulator = iosFake.isSimulator

        // Then
        assertEquals("iPhone 15 Pro", model)
        assertEquals(true, simulator)
        assertEquals(1, iosFake.getDeviceModelCallCount)
        assertEquals(1, iosFake.isSimulatorCallCount)
    }

    @Test
    fun `GIVEN iosTest WHEN using all three hierarchy levels THEN should access common and native and ios fakes`() {
        // Given - Using fakes from commonMain, nativeMain, AND iosMain
        val commonFake = fakeCommonPlatformService {
            log { level, message -> println("[$level] $message") }
            getPlatformName { "iOS" }
        }
        val nativeFake = fakeNativeOnlyService {
            architecture { "arm64" }
        }
        val iosFake = fakeIosOnlyService {
            getDeviceModel { "iPhone 15" }
        }

        // When
        commonFake.log("INFO", "Test from iOS")
        val platform = commonFake.getPlatformName()
        val arch = nativeFake.architecture
        val device = iosFake.getDeviceModel()

        // Then - All three levels work correctly
        assertEquals("iOS", platform)
        assertEquals("arm64", arch)
        assertEquals("iPhone 15", device)

        // Call tracking validation
        assertEquals(1, commonFake.logCallCount)
        assertEquals(1, commonFake.getPlatformNameCallCount)
        assertEquals(1, nativeFake.architectureCallCount)
        assertEquals(1, iosFake.getDeviceModelCallCount)
    }
}
