// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import sampleport.Point

class CinteropPortTest {
    @Test
    fun `GIVEN CinteropPort fake WHEN asking the origin THEN should return the configured point`() {
        // Given
        val fake = fakeCinteropPort {
            origin {
                cValue<Point> {
                    x = 3
                    y = 4
                }
            }
        }

        // When
        val point = fake.origin()

        // Then
        assertEquals(3, point?.useContents { x })
        assertEquals(4, point?.useContents { y })
        assertEquals(1, fake.originCalls.value.size)
    }

    @Test
    fun `GIVEN CinteropPort fake WHEN opening the forward-declared struct THEN should return null by default`() {
        // Given
        val fake = fakeCinteropPort()

        // When
        val handle = fake.open()

        // Then
        assertNull(handle)
        assertEquals(1, fake.openCalls.value.size)
    }
}
