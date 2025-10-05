// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.higherOrder

import com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.higherOrder.CallbackHandler
import com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.higherOrder.fakeCallbackHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for CallbackHandler SAM interface.
 */
class CallbackHandlerTest {
    @Test
    fun `GIVEN CallbackHandler SAM WHEN handling with callbacks THEN should invoke both`() {
        // Given
        val results = mutableListOf<String>()
        val handler =
            fakeCallbackHandler<Int> {
                handle { value, onSuccess, onError ->
                    if (value > 0) {
                        onSuccess(value.toString())
                    } else {
                        onError(IllegalArgumentException("Negative value"))
                    }
                }
            }

        // When - success case
        handler.handle(
            value = 42,
            onSuccess = { results.add(it) },
            onError = { },
        )

        // Then
        assertEquals(listOf("42"), results, "Should invoke success callback")
    }

    @Test
    fun `GIVEN CallbackHandler SAM WHEN error occurs THEN should invoke error callback`() {
        // Given
        val errors = mutableListOf<Throwable>()
        val handler =
            fakeCallbackHandler<Int> {
                handle { value, onSuccess, onError ->
                    if (value > 0) {
                        onSuccess(value.toString())
                    } else {
                        onError(IllegalArgumentException("Negative value"))
                    }
                }
            }

        // When - error case
        handler.handle(
            value = -1,
            onSuccess = { },
            onError = { errors.add(it) },
        )

        // Then
        assertEquals(1, errors.size, "Should invoke error callback")
        assertTrue(errors[0] is IllegalArgumentException)
    }
}
