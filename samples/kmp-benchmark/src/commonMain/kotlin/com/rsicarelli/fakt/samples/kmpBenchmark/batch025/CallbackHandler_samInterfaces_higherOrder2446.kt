// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch025

import com.rsicarelli.fakt.Fake

@Fake
fun interface CallbackHandler_samInterfaces_higherOrder2446<T> {
    fun handle(
        value: T,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit,
    )
}
