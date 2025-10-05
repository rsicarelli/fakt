// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.samInterfaces.stdlibTypes

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ErrorHandler SAM interface.
 */
class ErrorHandlerTest {
    @Test
    fun `GIVEN ErrorHandler SAM WHEN handling error THEN should return handled result`() {
        // Given
        val handler =
            fakeErrorHandler<String> {
                handle { result -> result.getOrNull() ?: "default" }
            }

        // When
        val successResult = handler.handle(Result.success("success"))
        val failureResult = handler.handle(Result.failure(Exception("error")))

        // Then
        assertEquals("success", successResult)
        assertEquals("default", failureResult)
    }
}
