// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch017

import com.rsicarelli.fakt.Fake

@Fake
fun interface CovariantListProducer_samInterfaces_variance1667<out T> {
    fun produce(): List<T>
}
