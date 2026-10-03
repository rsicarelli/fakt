// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInteropKmp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class DispatcherTest {

    @Test
    fun `GIVEN an sms WHEN dispatching THEN the notifier gets the stamped message`() = runTest {
        // Given
        val sent = mutableListOf<String>()
        val dispatcher =
            Dispatcher(
                notifier = fakeNotifier { send { _, message -> sent.add(message) } },
                log = fakeDeliveryLog(),
            )

        // When
        val delivered = dispatcher.dispatch(Channel.SMS, "hello")

        // Then
        assertTrue(delivered)
        assertEquals(listOf("interop:hello"), sent)
    }

    @Test
    fun `GIVEN an email WHEN dispatching THEN the log entry carries the channel priority`() =
        runTest {
            // Given
            val entries = mutableListOf<String>()
            val dispatcher =
                Dispatcher(
                    notifier = fakeNotifier { send { _, _ -> true } },
                    log = fakeDeliveryLog { record { entry -> entries.add(entry) } },
                )

            // When
            dispatcher.dispatch(Channel.EMAIL, "hi")

            // Then
            assertEquals(listOf("1:hi"), entries)
        }
}
