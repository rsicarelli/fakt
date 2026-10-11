// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import platform.posix.timespec

class PosixClockTest {
    @Test
    fun `GIVEN PosixClock fake WHEN reading now THEN should return the configured timespec`() {
        memScoped {
            // Given
            val ts = alloc<timespec>()
            ts.tv_sec = 42
            val fake = fakePosixClock { now { ts } }

            // When
            val result = fake.now()

            // Then
            assertEquals(42, result?.tv_sec)
            assertEquals(1, fake.nowCalls.value.size)
        }
    }

    @Test
    fun `GIVEN PosixClock fake WHEN opening a file THEN should return null by default`() {
        // Given
        val fake = fakePosixClock()

        // When
        val file = fake.open("/tmp/none")

        // Then
        assertNull(file)
        assertEquals(1, fake.openCalls.value.size)
    }

    @Test
    fun `GIVEN PosixClock fake WHEN closing a file THEN should return the configured status`() {
        // Given
        val fake = fakePosixClock { close { 0 } }

        // When
        val status = fake.close(null)

        // Then
        assertEquals(0, status)
        assertEquals(1, fake.closeCalls.value.size)
    }
}
