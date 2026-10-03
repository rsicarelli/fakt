// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInteropKmp

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmOnlyFilesTest {

    @Test
    fun `GIVEN an existing outbox WHEN labelling an sms THEN the label includes the stamp`() {
        // Given
        val files = fakeJvmOnlyFiles { exists { true } }

        // When
        val label = jvmChannelLabel(Channel.SMS, files)

        // Then
        assertEquals("interop:sms:true", label)
    }
}
