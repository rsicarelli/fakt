// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.LazyProvider
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.stdlib_types.fakeLazyProvider
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for LazyProvider SAM interface.
 */
class LazyProviderTest {
    @Test
    fun `GIVEN LazyProvider SAM WHEN providing lazily THEN should return lazy value`() {
        // Given
        val provider =
            fakeLazyProvider<String> {
                provide { lazy { "lazy-value" } }
            }

        // When
        val result = provider.provide()

        // Then
        assertEquals("lazy-value", result.value)
    }
}
