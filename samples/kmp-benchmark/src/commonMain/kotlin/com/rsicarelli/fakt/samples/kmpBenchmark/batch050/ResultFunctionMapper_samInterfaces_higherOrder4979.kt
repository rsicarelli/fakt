// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch050

import com.rsicarelli.fakt.Fake

@Fake
fun interface ResultFunctionMapper_samInterfaces_higherOrder4979<T, R> {
    fun mapResult(
        fn: (T) -> R,
        input: T,
    ): Result<R>
}
