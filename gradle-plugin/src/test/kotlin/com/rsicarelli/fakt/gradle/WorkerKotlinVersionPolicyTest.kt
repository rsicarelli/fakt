// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/** Truth table for the worker Kotlin version policy (#166); everything here is Project-free. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkerKotlinVersionPolicyTest {

    private val worker = "2.4.10"

    private fun verdict(user: String, workerVersion: String = worker) =
        workerKotlinVersionVerdict(user, workerVersion)

    @Test
    fun `GIVEN user Kotlin equal or older or higher patch WHEN judging THEN SUPPORTED`() {
        listOf("2.2.0", "2.3.20", "2.4.10", "2.4.21").forEach {
            assertEquals(WorkerKotlinVerdict.SUPPORTED, verdict(it), "user $it vs worker $worker")
        }
    }

    @Test
    fun `GIVEN user Kotlin one minor newer WHEN judging THEN NEWER_MINOR`() {
        listOf("2.5.0", "2.5.0-Beta1", "2.5.20").forEach {
            assertEquals(WorkerKotlinVerdict.NEWER_MINOR, verdict(it), "user $it vs worker $worker")
        }
    }

    @Test
    fun `GIVEN user Kotlin two minors or a major newer WHEN judging THEN UNSUPPORTED_NEWER`() {
        listOf("2.6.0", "2.6.0-Beta1", "3.0.0").forEach {
            assertEquals(
                WorkerKotlinVerdict.UNSUPPORTED_NEWER,
                verdict(it),
                "user $it vs worker $worker",
            )
        }
    }

    @Test
    fun `GIVEN an unparsable user version WHEN judging THEN UNKNOWN`() {
        listOf("garbage", "", "2").forEach {
            assertEquals(WorkerKotlinVerdict.UNKNOWN, verdict(it), "user '$it'")
        }
    }

    @Test
    fun `GIVEN an unparsable worker version WHEN judging THEN UNKNOWN`() {
        assertEquals(WorkerKotlinVerdict.UNKNOWN, verdict("2.5.0", "garbage"))
    }

    @Test
    fun `GIVEN worker 2_5_0 WHEN judging user versions THEN the window moves with the worker`() {
        assertEquals(WorkerKotlinVerdict.SUPPORTED, verdict("2.4.10", "2.5.0"))
        assertEquals(WorkerKotlinVerdict.NEWER_MINOR, verdict("2.6.0", "2.5.0"))
        assertEquals(WorkerKotlinVerdict.UNSUPPORTED_NEWER, verdict("2.7.0", "2.5.0"))
    }

    @Test
    fun `GIVEN a newer major with a lower minor WHEN judging THEN UNSUPPORTED_NEWER`() {
        assertEquals(WorkerKotlinVerdict.UNSUPPORTED_NEWER, verdict("3.0.0", "2.9.0"))
    }

    @Test
    fun `GIVEN version strings with suffixes WHEN parsing THEN major minor patch are read`() {
        assertEquals(KotlinVersionParts(2, 5, 0), parseKotlinVersion("2.5.0-Beta1"))
        assertEquals(KotlinVersionParts(2, 4, 20), parseKotlinVersion("2.4.20-RC3"))
        assertEquals(KotlinVersionParts(2, 5, 20), parseKotlinVersion("2.5.20-dev-1234"))
        assertEquals(KotlinVersionParts(2, 4, 0), parseKotlinVersion("2.4.0-titan-100"))
        assertEquals(KotlinVersionParts(2, 4, 10), parseKotlinVersion("2.4.10"))
        assertEquals(KotlinVersionParts(2, 5, 0), parseKotlinVersion("2.5"))
    }

    @Test
    fun `GIVEN strings without major and minor WHEN parsing THEN null`() {
        listOf("", "2", "garbage", "v2.4.10", ".4.1").forEach { assertNull(parseKotlinVersion(it)) }
    }

    @Test
    fun `GIVEN a one minor newer user WHEN building the warning THEN both versions and advice appear`() {
        val message = newerMinorWarning(user = "2.5.0", worker = worker)

        assertTrue("2.5.0" in message && worker in message, message)
        assertTrue("Worker compiler version" in message, message)
    }

    @Test
    fun `GIVEN a two minors newer user WHEN building the error THEN it names versions and the fix`() {
        val message = unsupportedNewerError(user = "2.6.0", worker = worker)

        assertTrue("2.6.0" in message && worker in message, message)
        assertTrue("faktWorker" in message, message)
        assertTrue("fakt.allowNewerKotlin" in message, message)
        assertTrue("Compatibility > Worker compiler version" in message, message)
    }

    @Test
    fun `GIVEN two minors or a newer major WHEN building the error THEN it states the real rule`() {
        listOf("2.6.0", "3.0.0").forEach { user ->
            val message = unsupportedNewerError(user = user, worker = worker)

            assertTrue("at most one minor version newer" in message, message)
            assertFalse("two or more minor versions older" in message, message)
        }
    }

    @Test
    fun `GIVEN one minor newer WHEN building the warning THEN it keeps the phrase the CI canary greps`() {
        val message = newerMinorWarning(user = "2.5.0", worker = worker)

        assertTrue("one minor newer than the Kotlin" in message, message)
    }

    @Test
    fun `GIVEN a two minors newer user WHEN building the error THEN it is honest about the fix`() {
        val message = unsupportedNewerError(user = "2.6.0", worker = worker)

        assertTrue("does not clear this check" in message, message)
        assertFalse("Update Fakt to a release built for" in message, message)
    }

    @Test
    fun `GIVEN allowNewer WHEN building the downgraded warning THEN it does not ask to set the property`() {
        val message = allowedNewerWarning(user = "2.6.0", worker = worker)

        assertTrue("2.6.0" in message && worker in message, message)
        assertFalse("To build anyway" in message, message)
        assertTrue("fakt.allowNewerKotlin=true" in message, message)
    }

    @Test
    fun `GIVEN raw property values WHEN reading allowNewerKotlin THEN only true enables it`() {
        assertTrue(allowNewerKotlin("true"))
        assertTrue(allowNewerKotlin("TRUE"))
        assertFalse(allowNewerKotlin("false"))
        assertFalse(allowNewerKotlin("yes"))
        assertFalse(allowNewerKotlin(""))
        assertFalse(allowNewerKotlin(null))
    }

    @Test
    fun `GIVEN each verdict WHEN allowNewer is false THEN outcome is silent warn or fail`() {
        fun outcome(user: String) = workerKotlinOutcome(user, worker, allowNewer = false)

        assertEquals(WorkerKotlinOutcome.Silent, outcome("2.4.21"))
        assertEquals(WorkerKotlinOutcome.Silent, outcome("garbage"))
        assertEquals(WorkerKotlinOutcome.Warn(newerMinorWarning("2.5.0", worker)), outcome("2.5.0"))
        assertEquals(
            WorkerKotlinOutcome.Fail(unsupportedNewerError("2.6.0", worker)),
            outcome("2.6.0"),
        )
    }

    @Test
    fun `GIVEN allowNewer true WHEN the user is two minors newer THEN warn not fail`() {
        val outcome = workerKotlinOutcome("2.6.0", worker, allowNewer = true)

        assertEquals(WorkerKotlinOutcome.Warn(allowedNewerWarning("2.6.0", worker)), outcome)
    }

    @Test
    fun `GIVEN allowNewer true WHEN the user is supported or unknown THEN still silent`() {
        assertEquals(WorkerKotlinOutcome.Silent, workerKotlinOutcome("2.4.10", worker, true))
        assertEquals(WorkerKotlinOutcome.Silent, workerKotlinOutcome("garbage", worker, true))
    }
}
