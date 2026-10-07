// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals

class WebStorageTest {

    @Test
    fun `GIVEN a webMain fake WHEN reading a key THEN the configured value is returned`() {
        // Given
        val storage = fakeWebStorage {
            device { DeviceInfo(name = "browser", isEmulator = false) }
            read { key -> "${key.namespace}/${key.name}" }
        }

        // When
        val value = storage.read(StorageKey("prefs", "theme"))

        // Then
        assertEquals("prefs/theme", value)
        assertEquals("browser", storage.device.name)
        assertEquals(1, storage.readCalls.value.size)
    }

    @Test
    fun `GIVEN a webMain fake WHEN writing a key THEN the call is tracked`() {
        // Given
        val storage = fakeWebStorage()

        // When
        storage.write(StorageKey("prefs", "theme"), "dark")

        // Then
        assertEquals(1, storage.writeCalls.value.size)
    }

    @Test
    fun `GIVEN a web target WHEN asking the runtime family THEN it is web`() {
        // Given / When
        val family = runtimeFamily()

        // Then
        assertEquals("web", family)
    }
}
