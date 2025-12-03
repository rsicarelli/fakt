// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch028

import com.rsicarelli.fakt.Fake

data class MathOperations_operators_basic2757_1(val x: Double, val y: Double)

@Fake
interface MathOperations_operators_basic2757 {
    
    operator fun MathOperations_operators_basic2757_1.plus(other: MathOperations_operators_basic2757_1): MathOperations_operators_basic2757_1

    
    operator fun MathOperations_operators_basic2757_1.minus(other: MathOperations_operators_basic2757_1): MathOperations_operators_basic2757_1

    
    operator fun MathOperations_operators_basic2757_1.times(scalar: Double): MathOperations_operators_basic2757_1

    
    operator fun MathOperations_operators_basic2757_1.div(scalar: Double): MathOperations_operators_basic2757_1

    
    operator fun MathOperations_operators_basic2757_1.unaryMinus(): MathOperations_operators_basic2757_1

    
    operator fun MathOperations_operators_basic2757_1.inc(): MathOperations_operators_basic2757_1
}
