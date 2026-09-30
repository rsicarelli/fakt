// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import kotlin.test.Test
import kotlin.test.assertNull

class AppleFakesTest {
    @Test
    fun GIVEN_apple_fake_WHEN_resolve_THEN_default_null() {
        assertNull(fakeAppleUrls().resolve("x"))
    }
}
