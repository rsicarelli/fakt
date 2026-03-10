// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsConstraints

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConverterTest {
    @Test
    fun `GIVEN converter with String to Int WHEN toDomain THEN returns configured value`() {
        // Given
        val converter = fakeConverter<String, Int> {
            toDomain { network -> network.length }
        }

        // When
        val result: Int? = converter.toDomain("hello")

        // Then
        assertEquals(5, result)
    }

    @Test
    fun `GIVEN converter with defaults WHEN toDomain THEN returns null`() {
        // Given
        val converter = fakeConverter<String, Int>()

        // When
        val result: Int? = converter.toDomain("hello")

        // Then
        assertNull(result)
    }

    @Test
    fun `GIVEN converter WHEN verifyToDomain THEN tracks calls correctly`() {
        // Given
        val converter = fakeConverter<String, Int> {
            toDomain { network -> network.length }
        }

        // When
        converter.toDomain("hello")
        converter.toDomain("world")

        // Then
        converter.verifyToDomain {
            wasCalledTimes(2)
        }
    }

    @Test
    fun `GIVEN converter WHEN verifyClear THEN tracks calls correctly`() {
        // Given
        val converter = fakeConverter<String, Int>()

        // When
        converter.clear()

        // Then
        converter.verifyClear {
            wasCalledTimes(1)
        }
    }
}
