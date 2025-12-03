// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch084

import com.rsicarelli.fakt.Fake

@Fake
fun interface ResultFunctionHandler_samInterfaces_stdlibTypes8327<T, R> {
    fun handle(
        result: Result<T>,
        mapper: (T) -> R,
    ): Result<R>
}
