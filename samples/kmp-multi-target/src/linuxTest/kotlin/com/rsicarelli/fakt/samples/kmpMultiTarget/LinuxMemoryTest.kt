// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr

class LinuxMemoryTest {
    @Test
    fun `GIVEN LinuxMemory fake WHEN allocating THEN should return the configured pointer`() {
        // Given
        val byte = nativeHeap.alloc<ByteVar>()
        val pointer: CPointer<ByteVar> = byte.ptr
        val fake = fakeLinuxMemory { allocate { pointer } }

        // When
        val result = fake.allocate(16u)

        // Then
        assertEquals(pointer, result)
        assertEquals(1, fake.allocateCalls.value.size)
        nativeHeap.free(byte)
    }

    @Test
    fun `GIVEN LinuxMemory fake WHEN using defaults THEN should return null and track calls`() {
        // Given
        val fake = fakeLinuxMemory()

        // When
        val result = fake.allocate(8u)
        fake.release(result)

        // Then
        assertNull(result)
        assertEquals(1, fake.allocateCalls.value.size)
        assertEquals(1, fake.releaseCalls.value.size)
    }
}
