// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsConstraints

import com.rsicarelli.fakt.Fake

/**
 * Regression test for #61: Generic interface verify functions missing type bounds.
 *
 * Tests that bounded type parameters (`: Any`) are preserved in generated verify extension
 * functions.
 */
@Fake
interface Converter<NetworkType : Any, LocalType : Any> {
    fun toDomain(network: NetworkType): LocalType?

    fun clear()
}
