// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.transform

import com.rsicarelli.fakt.compiler.fir.metadata.FirCallHistoryMode
import com.rsicarelli.fakt.compiler.fir.metadata.resolveCallHistory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance

/**
 * Tests for call history resolution logic.
 *
 * The resolution priority is:
 * 1. If annotation specifies ENABLED → true (override plugin default)
 * 2. If annotation specifies DISABLED → false (override plugin default)
 * 3. If annotation specifies DEFAULT → follow plugin default
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallHistoryResolutionTest {
    private fun resolve(mode: FirCallHistoryMode, pluginDefault: Boolean): Boolean =
        mode.resolveCallHistory(pluginDefault)

    // ==========================================
    // ENABLED mode tests - always returns true
    // ==========================================

    @Test
    fun `GIVEN annotation ENABLED and plugin default true WHEN resolving THEN returns true`() =
        runTest {
            // GIVEN
            val mode = FirCallHistoryMode.ENABLED
            val pluginDefault = true

            // WHEN
            val result = resolve(mode, pluginDefault)

            // THEN - ENABLED always returns true
            assertTrue(result, "ENABLED mode should always return true")
        }

    @Test
    fun `GIVEN annotation ENABLED and plugin default false WHEN resolving THEN returns true`() =
        runTest {
            // GIVEN
            val mode = FirCallHistoryMode.ENABLED
            val pluginDefault = false

            // WHEN
            val result = resolve(mode, pluginDefault)

            // THEN - ENABLED overrides plugin default
            assertTrue(result, "ENABLED mode should override plugin default false")
        }

    // ==========================================
    // DISABLED mode tests - always returns false
    // ==========================================

    @Test
    fun `GIVEN annotation DISABLED and plugin default true WHEN resolving THEN returns false`() =
        runTest {
            // GIVEN
            val mode = FirCallHistoryMode.DISABLED
            val pluginDefault = true

            // WHEN
            val result = resolve(mode, pluginDefault)

            // THEN - DISABLED overrides plugin default
            assertFalse(result, "DISABLED mode should override plugin default true")
        }

    @Test
    fun `GIVEN annotation DISABLED and plugin default false WHEN resolving THEN returns false`() =
        runTest {
            // GIVEN
            val mode = FirCallHistoryMode.DISABLED
            val pluginDefault = false

            // WHEN
            val result = resolve(mode, pluginDefault)

            // THEN - DISABLED always returns false
            assertFalse(result, "DISABLED mode should always return false")
        }

    // ==========================================
    // DEFAULT mode tests - follows plugin default
    // ==========================================

    @Test
    fun `GIVEN annotation DEFAULT and plugin default true WHEN resolving THEN returns true`() =
        runTest {
            // GIVEN
            val mode = FirCallHistoryMode.DEFAULT
            val pluginDefault = true

            // WHEN
            val result = resolve(mode, pluginDefault)

            // THEN - DEFAULT follows plugin default
            assertTrue(result, "DEFAULT mode should follow plugin default true")
        }

    @Test
    fun `GIVEN annotation DEFAULT and plugin default false WHEN resolving THEN returns false`() =
        runTest {
            // GIVEN
            val mode = FirCallHistoryMode.DEFAULT
            val pluginDefault = false

            // WHEN
            val result = resolve(mode, pluginDefault)

            // THEN - DEFAULT follows plugin default
            assertFalse(result, "DEFAULT mode should follow plugin default false")
        }

    // ==========================================
    // Edge cases and combinations
    // ==========================================

    @Test
    fun `GIVEN all modes WHEN plugin default is true THEN ENABLED and DEFAULT return true, DISABLED returns false`() =
        runTest {
            // GIVEN
            val pluginDefault = true

            // WHEN/THEN
            assertTrue(resolve(FirCallHistoryMode.ENABLED, pluginDefault))
            assertTrue(resolve(FirCallHistoryMode.DEFAULT, pluginDefault))
            assertFalse(resolve(FirCallHistoryMode.DISABLED, pluginDefault))
        }

    @Test
    fun `GIVEN all modes WHEN plugin default is false THEN only ENABLED returns true`() = runTest {
        // GIVEN
        val pluginDefault = false

        // WHEN/THEN
        assertTrue(resolve(FirCallHistoryMode.ENABLED, pluginDefault))
        assertFalse(resolve(FirCallHistoryMode.DEFAULT, pluginDefault))
        assertFalse(resolve(FirCallHistoryMode.DISABLED, pluginDefault))
    }
}
