// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch062

import com.rsicarelli.fakt.Fake

@Fake
fun interface ActionWrapper_samInterfaces_higherOrder6148<T> {
    fun wrap(action: (T) -> Unit): (T) -> Unit
}
