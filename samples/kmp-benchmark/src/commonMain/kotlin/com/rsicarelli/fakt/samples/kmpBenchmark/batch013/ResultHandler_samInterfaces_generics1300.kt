// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch013

import com.rsicarelli.fakt.Fake

@Fake
fun interface ResultHandler_samInterfaces_generics1300<T> {
    fun handle(input: T): Result<T>
}
