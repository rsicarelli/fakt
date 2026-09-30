// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import kotlin.test.Test
import kotlin.test.assertEquals

class GreeterTest {
    @Test
    fun GIVEN_common_fake_WHEN_greet_THEN_uses_behavior() {
        assertEquals("hi x", fakeGreeter { greet { "hi $it" } }.greet("x"))
    }
}
