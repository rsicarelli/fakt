// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics

import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.MultiConstraintHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.sam_interfaces.generics.fakeMultiConstraintHandler
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for MultiConstraintHandler SAM interface.
 */
class MultiConstraintHandlerTest {
    @Test
    fun `GIVEN SAM with multiple constraints WHEN creating fake THEN should compile`() {
        // Given
        val handler =
            fakeMultiConstraintHandler<String> {
                handle { item -> item.length }
            }

        // When
        val result = handler.handle("hello")

        // Then
        assertEquals(5, result, "Should handle multi-constrained type")
    }
}
