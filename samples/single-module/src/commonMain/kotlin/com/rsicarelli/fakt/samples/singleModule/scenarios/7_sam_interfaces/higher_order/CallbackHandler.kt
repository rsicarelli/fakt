// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.higher_order

import com.rsicarelli.fakt.Fake

/**
 * Scenario 42c: Callback handler with success/error callbacks
 */
@Fake
fun interface CallbackHandler<T> {
    fun handle(value: T, onSuccess: (String) -> Unit, onError: (Throwable) -> Unit)
}
