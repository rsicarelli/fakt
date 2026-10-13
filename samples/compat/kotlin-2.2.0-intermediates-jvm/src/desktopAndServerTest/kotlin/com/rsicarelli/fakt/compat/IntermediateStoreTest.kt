// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.compat

import kotlin.test.Test
import kotlin.test.assertEquals

class IntermediateStoreTest {

    @Test
    fun `GIVEN fake from intermediate WHEN customizing behavior THEN applies correctly`() {
        val fake = fakeIntermediateStore { open { user -> "token-$user" } }
        assertEquals("token-alice", fake.open("alice"))
    }
}
