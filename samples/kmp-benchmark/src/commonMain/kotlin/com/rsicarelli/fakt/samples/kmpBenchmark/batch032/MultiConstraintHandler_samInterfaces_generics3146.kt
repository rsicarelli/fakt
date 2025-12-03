// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch032

import com.rsicarelli.fakt.Fake

@Fake
fun interface MultiConstraintHandler_samInterfaces_generics3146<T> where T : CharSequence, T : Comparable<T> {
    fun handle(item: T): Int
}
