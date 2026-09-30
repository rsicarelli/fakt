// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinuxFakesTest {
    @Test
    fun GIVEN_posix_fake_WHEN_configured_THEN_returns_behavior() {
        val clock = fakePosixClock { clockId { 7 } }
        assertEquals(7, clock.clockId)
        assertNull(clock.now())
        assertNull(fakeCinteropPort().newOpaque())
    }
}
