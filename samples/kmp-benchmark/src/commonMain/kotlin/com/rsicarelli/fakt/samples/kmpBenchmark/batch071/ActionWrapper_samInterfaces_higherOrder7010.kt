// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch071

import com.rsicarelli.fakt.Fake

@Fake
fun interface ActionWrapper_samInterfaces_higherOrder7010<T> {
    fun wrap(action: (T) -> Unit): (T) -> Unit
}
