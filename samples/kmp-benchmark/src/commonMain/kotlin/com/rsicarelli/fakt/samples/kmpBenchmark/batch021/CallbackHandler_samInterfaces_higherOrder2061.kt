// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch021

import com.rsicarelli.fakt.Fake

@Fake
fun interface CallbackHandler_samInterfaces_higherOrder2061<T> {
    fun handle(
        value: T,
        onSuccess: (String) -> Unit,
        onError: (Throwable) -> Unit,
    )
}
