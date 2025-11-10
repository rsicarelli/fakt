// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.operators.basic

import kotlin.test.Test
import kotlin.test.assertEquals

class MathOperationsTest {
    @Test
    fun `GIVEN MathOperations fake WHEN using plus operator THEN should add vectors`() {
        // Given
        val fake = fakeMathOperations {
            plus { receiver, other ->
                Vector(receiver.x + other.x, receiver.y + other.y)
            }
        }

        // When
        val v1 = Vector(1.0, 2.0)
        val v2 = Vector(3.0, 4.0)
        val result = with(fake) { v1 + v2 } // Operator syntax!

        // Then
        assertEquals(Vector(4.0, 6.0), result)
    }

    @Test
    fun `GIVEN MathOperations fake WHEN using minus operator THEN should subtract vectors`() {
        // Given
        val fake = fakeMathOperations {
            minus { receiver, other ->
                Vector(receiver.x - other.x, receiver.y - other.y)
            }
        }

        // When
        val v1 = Vector(5.0, 7.0)
        val v2 = Vector(2.0, 3.0)
        val result = with(fake) { v1 - v2 } // Operator syntax!

        // Then
        assertEquals(Vector(3.0, 4.0), result)
    }

    @Test
    fun `GIVEN MathOperations fake WHEN using times operator THEN should multiply by scalar`() {
        // Given
        val fake = fakeMathOperations {
            times { receiver, scalar ->
                Vector(receiver.x * scalar, receiver.y * scalar)
            }
        }

        // When
        val v = Vector(2.0, 3.0)
        val result = with(fake) { v * 2.0 } // Operator syntax!

        // Then
        assertEquals(Vector(4.0, 6.0), result)
    }

    @Test
    fun `GIVEN MathOperations fake WHEN using div operator THEN should divide by scalar`() {
        // Given
        val fake = fakeMathOperations {
            div { receiver, scalar ->
                Vector(receiver.x / scalar, receiver.y / scalar)
            }
        }

        // When
        val v = Vector(10.0, 20.0)
        val result = with(fake) { v / 2.0 } // Operator syntax!

        // Then
        assertEquals(Vector(5.0, 10.0), result)
    }

    @Test
    fun `GIVEN MathOperations fake WHEN using unaryMinus operator THEN should negate vector`() {
        // Given
        val fake = fakeMathOperations {
            unaryMinus { receiver ->
                Vector(-receiver.x, -receiver.y)
            }
        }

        // When
        val v = Vector(3.0, -4.0)
        val result = with(fake) { -v } // Operator syntax!

        // Then
        assertEquals(Vector(-3.0, 4.0), result)
    }

    @Test
    fun `GIVEN MathOperations fake WHEN using inc operator THEN should increment vector`() {
        // Given
        val fake = fakeMathOperations {
            inc { receiver ->
                Vector(receiver.x + 1.0, receiver.y + 1.0)
            }
        }

        // When
        val v = Vector(1.0, 2.0)
        val result = with(fake) {
            var mutableV = v
            ++mutableV
            mutableV
        }

        // Then
        assertEquals(Vector(2.0, 3.0), result)
    }

    @Test
    fun `GIVEN MathOperations fake WHEN using defaults THEN should use default behavior`() {
        // Given
        val fake = fakeMathOperations() // No configuration

        // When/Then
        // This test will fail until we implement operator support
        // Expected: Operators should have sensible defaults (e.g., error() or identity)
        // For now, this documents the expected behavior
    }

    @Test
    fun `GIVEN MathOperations fake WHEN combining operators THEN should work together`() {
        // Given
        val fake = fakeMathOperations {
            plus { receiver, other ->
                Vector(receiver.x + other.x, receiver.y + other.y)
            }
            times { receiver, scalar ->
                Vector(receiver.x * scalar, receiver.y * scalar)
            }
        }

        // When
        val v1 = Vector(1.0, 2.0)
        val v2 = Vector(3.0, 4.0)
        val result = with(fake) {
            (v1 + v2) * 2.0 // Chained operators!
        }

        // Then
        assertEquals(Vector(8.0, 12.0), result)
    }
}
