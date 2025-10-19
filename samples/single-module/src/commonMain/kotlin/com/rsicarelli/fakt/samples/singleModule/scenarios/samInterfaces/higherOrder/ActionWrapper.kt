// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.higherOrder

import com.rsicarelli.fakt.Fake

/**
 * Scenario 41: Action wrapper
 */
@Fake
fun interface ActionWrapper<T> {
    fun wrap(action: (T) -> Unit): (T) -> Unit
}
