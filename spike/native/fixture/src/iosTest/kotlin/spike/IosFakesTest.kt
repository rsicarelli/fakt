// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import kotlin.test.Test
import kotlin.test.assertNull

class IosFakesTest {
    @Test
    fun GIVEN_ios_fake_WHEN_root_THEN_default_null() {
        assertNull(fakeIosViews().root())
    }
}
