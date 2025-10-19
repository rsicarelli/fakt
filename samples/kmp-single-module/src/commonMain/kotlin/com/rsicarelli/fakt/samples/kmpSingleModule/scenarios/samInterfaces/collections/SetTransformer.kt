// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.Fake

/**
 * Scenario 28a: Set transformer
 */
@Fake
fun interface SetTransformer<T> {
    fun transform(items: Set<T>): Set<T>
}
