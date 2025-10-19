// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic

import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic.VoidAction
import com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.samInterfaces.basic.fakeVoidAction
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for VoidAction SAM interface.
 */
class VoidActionTest {
    @Test
    fun `GIVEN SAM with Unit return WHEN configuring THEN should execute action`() {
        // Given
        var executedCommand = ""
        val action =
            fakeVoidAction {
                execute { command -> executedCommand = command }
            }

        // When
        action.execute("test-command")

        // Then
        assertEquals("test-command", executedCommand, "Should execute command")
    }
}
