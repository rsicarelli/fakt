// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import kotlin.test.Test
import kotlin.test.assertNull

class NativeFakesTest {
    @Test
    fun GIVEN_native_fake_WHEN_alloc_THEN_default_null() {
        assertNull(fakeNativeMemory().alloc(8u))
    }
}
