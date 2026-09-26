// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

/**
 * Drives the commonTest fakes of a single-target project. Before issue #153 these came from the
 * in-process compiler plugin; now `faktGenerateJvmMain` owns them as declared task outputs. If the
 * common half were dropped or routed to jvmTest, this file would not compile.
 */
class SessionManagerTest {

    @Test
    fun `GIVEN a stored session WHEN resuming THEN returns it and tracks the resume`() = runTest {
        // Given
        val stored = Session(id = "s-1", userName = "ada")
        val repository = fakeSessionRepository { load { id -> stored.takeIf { it.id == id } } }
        val tracker = fakeAnalyticsTracker()
        val manager = SessionManager(repository, tracker)

        // When
        val resumed = manager.resume("s-1")

        // Then
        assertEquals(stored, resumed)
        assertEquals(listOf("session_resumed"), tracker.trackCalls.value.map { it.event })
    }

    @Test
    fun `GIVEN no stored session WHEN resuming THEN returns null and tracks the miss`() = runTest {
        // Given
        val tracker = fakeAnalyticsTracker()
        val manager = SessionManager(fakeSessionRepository(), tracker)

        // When
        val resumed = manager.resume("unknown")

        // Then
        assertNull(resumed)
        assertEquals(listOf("session_missing"), tracker.trackCalls.value.map { it.event })
    }

    @Test
    fun `GIVEN the jvm actual WHEN signing in THEN the session id carries the platform name`() =
        runTest {
            // Given
            val repository = fakeSessionRepository { save { true } }
            val manager = SessionManager(repository, fakeAnalyticsTracker())

            // When
            val session = manager.signIn("grace")

            // Then
            assertEquals("jvm-grace", session.id)
            assertEquals(listOf(session), repository.saveCalls.value.map { it.session })
        }
}
