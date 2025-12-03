// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch077

import com.rsicarelli.fakt.Fake

@Fake
fun interface ComplexBoundHandler_samInterfaces_edgeCases7652<T> where T : CharSequence, T : Comparable<T> {
    fun handle(item: T): Int
}
