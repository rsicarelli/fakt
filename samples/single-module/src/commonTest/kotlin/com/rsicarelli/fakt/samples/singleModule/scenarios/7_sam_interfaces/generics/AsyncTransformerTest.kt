// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.generics

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for AsyncTransformer SAM interface.
 */
class AsyncTransformerTest {
    @Test
    fun `GIVEN SAM with suspend generic WHEN transforming async THEN should work in coroutines`() =
        runTest {
            // Given
            val transformer =
                fakeAsyncTransformer<Int> {
                    transform { input -> input * 2 }
                }

            // When
            val result = transformer.transform(21)

            // Then
            assertEquals(42, result, "Should transform value asynchronously")
        }
}
