// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.generics

import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.generics.fakeMultiConstraintHandler
import kotlin.test.Test
import kotlin.test.assertEquals

class MultiConstraintHandlerTest {
    @Test
    fun `GIVEN SAM with multiple constraints WHEN creating fake THEN should compile`() {
        // Given
        val handler = fakeMultiConstraintHandler<String> {
            handle { item -> item.length }
        }

        // When
        val result = handler.handle("hello")

        // Then
        assertEquals(5, result, "Should handle multi-constrained type")
    }
}
