// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch012

import com.rsicarelli.fakt.Fake

@Fake
fun interface AsyncProducer_samInterfaces_variance1152<out T> {
    suspend fun produce(): T
}
