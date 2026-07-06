// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * Drives the fake generated from [DeviceSessionService] — an interface whose signatures use an
 * expect type at every depth. If the commonMain producer failed on the unpaired expects
 * (the issue #79 blocker) this file would not compile on any target.
 */
class DeviceSessionServiceTest {

    @Test
    fun `GIVEN fake WHEN expect type used as property and return THEN configured devices are returned`() {
        // Given
        val fake = fakeDeviceSessionService {
            device { currentDevice() }
            activeDevice { DeviceInfo(name = "active", isEmulator = true) }
        }

        // When
        val property = fake.device
        val returned = fake.activeDevice()

        // Then
        assertTrue(property.name.isNotEmpty())
        assertEquals("active", returned.name)
        assertTrue(returned.isEmulator)
    }

    @Test
    fun `GIVEN fake WHEN expect type used as parameter THEN behavior receives it and calls are tracked`() {
        // Given
        val fake = fakeDeviceSessionService { bind { device -> device.isEmulator } }

        // When
        val bound = fake.bind(DeviceInfo(name = "test-rig", isEmulator = false))

        // Then
        assertFalse(bound)
        assertEquals(1, fake.bindCalls.value.size)
    }

    @Test
    fun `GIVEN fake WHEN nullable return and generic argument depths THEN defaults apply`() {
        // Given
        val fake = fakeDeviceSessionService()

        // When
        val lastKnown = fake.lastKnownDevice()
        val history = fake.history()

        // Then
        assertNull(lastKnown, "Nullable expect-typed return must default to null")
        assertTrue(history.isEmpty(), "List<DeviceInfo> must default to an empty list")
    }

    @Test
    fun `GIVEN fake WHEN suspend variant with expect type THEN configured behavior runs`() = runTest {
        // Given
        val fake = fakeDeviceSessionService {
            refresh { device -> DeviceInfo(name = device.name, isEmulator = true) }
        }

        // When
        val refreshed = fake.refresh(DeviceInfo(name = "sensor", isEmulator = false))

        // Then
        assertEquals("sensor", refreshed.name)
        assertTrue(refreshed.isEmulator)
        assertEquals(1, fake.refreshCalls.value.size)
    }
}
