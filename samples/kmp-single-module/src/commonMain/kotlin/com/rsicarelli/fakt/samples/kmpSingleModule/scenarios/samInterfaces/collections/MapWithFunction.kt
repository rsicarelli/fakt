// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.collections

import com.rsicarelli.fakt.Fake

/**
 * Scenario 27: Map with function parameter
 */
@Fake
fun interface MapWithFunction<T, R> {
    fun transform(
        items: List<T>,
        mapper: (T) -> R,
    ): List<R>
}
