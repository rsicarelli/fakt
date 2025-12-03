// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch069

import com.rsicarelli.fakt.Fake

@Fake
fun interface MultiConstraintHandler_samInterfaces_generics6837<T> where T : CharSequence, T : Comparable<T> {
    fun handle(item: T): Int
}
