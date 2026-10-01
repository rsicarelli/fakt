// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInteropKmp

import kotlin.test.Test
import kotlin.test.assertEquals

class JsOnlyStorageTest {

    @Test
    fun `GIVEN a stored outbox WHEN labelling an email THEN the label includes the stamp`() {
        // Given
        val storage = fakeJsOnlyStorage { read { "queued" } }

        // When
        val label = jsChannelLabel(Channel.EMAIL, storage)

        // Then
        assertEquals("interop:email:queued", label)
    }
}
