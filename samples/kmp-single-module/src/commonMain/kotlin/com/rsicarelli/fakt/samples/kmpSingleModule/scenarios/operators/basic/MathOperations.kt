// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.operators.basic

import com.rsicarelli.fakt.Fake

/**
 * Data class representing a 2D vector for operator testing.
 */
data class Vector(val x: Double, val y: Double)

/**
 * Test interface for basic math operator functions.
 *
 * Covers: plus (+), minus (-), times (*), div (/)
 *
 * Expected behavior:
 * - Generated fake should support operator syntax: v1 + v2, v1 * 2.0, etc.
 * - Default behavior can be configured via DSL
 * - Call tracking should work normally
 */
@Fake
interface MathOperations {
    /**
     * Vector addition operator.
     * Usage: v1 + v2
     */
    operator fun Vector.plus(other: Vector): Vector

    /**
     * Vector subtraction operator.
     * Usage: v1 - v2
     */
    operator fun Vector.minus(other: Vector): Vector

    /**
     * Scalar multiplication operator.
     * Usage: v * 2.0
     */
    operator fun Vector.times(scalar: Double): Vector

    /**
     * Scalar division operator.
     * Usage: v / 2.0
     */
    operator fun Vector.div(scalar: Double): Vector

    /**
     * Unary minus operator.
     * Usage: -v
     */
    operator fun Vector.unaryMinus(): Vector

    /**
     * Increment operator.
     * Usage: v++
     */
    operator fun Vector.inc(): Vector
}
