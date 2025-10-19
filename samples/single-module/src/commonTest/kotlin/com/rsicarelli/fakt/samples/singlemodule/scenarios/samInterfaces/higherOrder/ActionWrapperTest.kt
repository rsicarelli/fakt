// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.higherOrder

import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.higherOrder.ActionWrapper
import com.rsicarelli.fakt.samples.singlemodule.scenarios.samInterfaces.higherOrder.fakeActionWrapper
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for ActionWrapper SAM interface.
 */
class ActionWrapperTest {
    @Test
    fun `GIVEN ActionWrapper SAM WHEN wrapping action THEN should execute wrapped action`() {
        // Given
        var executionLog = mutableListOf<String>()
        val wrapper =
            fakeActionWrapper<String> {
                wrap { action ->
                    { input: String ->
                        executionLog.add("before-$input")
                        action(input)
                        executionLog.add("after-$input")
                    }
                }
            }

        // When
        var executed = ""
        val wrappedAction = wrapper.wrap { value -> executed = value }
        wrappedAction("test")

        // Then
        assertEquals("test", executed)
        assertEquals(listOf("before-test", "after-test"), executionLog)
    }
}
