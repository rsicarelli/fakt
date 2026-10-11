// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpNativeOnly.allNative

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NativeBufferTest {
    @Test
    fun `GIVEN NativeBuffer fake WHEN reading capacity THEN should return the configured size`() {
        // Given
        val fake = fakeNativeBuffer { capacity { 64u } }

        // When
        val result = fake.capacity()

        // Then
        assertEquals(64u, result)
        assertEquals(1, fake.capacityCalls.value.size)
    }

    @Test
    fun `GIVEN NativeBuffer fake WHEN resizing with defaults THEN should return false and track the call`() {
        // Given
        val fake = fakeNativeBuffer()

        // When
        val resized = fake.resize(128u)

        // Then
        assertFalse(resized)
        assertEquals(1, fake.resizeCalls.value.size)
    }

    @Test
    fun `GIVEN NativeBuffer fake WHEN reading the label THEN should return the configured value`() {
        // Given
        val fake = fakeNativeBuffer { label { "scratch" } }

        // When
        val label = fake.label

        // Then
        assertEquals("scratch", label)
    }
}
