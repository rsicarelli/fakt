// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.inheritance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoggingServiceTest {

    @Test
    fun `GIVEN unconfigured start WHEN called THEN throws error`() {
        val service: LoggingService = fakeLoggingService {}

        assertFailsWith<IllegalStateException> {
            service.start()
        }
    }

    @Test
    fun `GIVEN configured start WHEN called THEN uses custom behavior`() {
        var called = false

        val service: LoggingService = fakeLoggingService {
            start {
                called = true
                false
            }
        }

        val result = service.start()

        assertTrue(called)
        assertEquals(false, result)
    }

    @Test
    fun `GIVEN unconfigured log WHEN called THEN throws error`() {
        val service: LoggingService = fakeLoggingService {}

        assertFailsWith<IllegalStateException> {
            service.log("test")
        }
    }

    @Test
    fun `GIVEN configured log WHEN called THEN uses custom behavior`() {
        val messages = mutableListOf<String>()

        val service: LoggingService = fakeLoggingService {
            log { msg -> messages.add(msg) }
        }

        service.log("test1")
        service.log("test2")

        assertEquals(listOf("test1", "test2"), messages)
    }

    @Test
    fun `GIVEN unconfigured getLogLevel WHEN called THEN uses super implementation`() {
        val service: LoggingService = fakeLoggingService {}

        val level = service.getLogLevel()

        assertEquals("INFO", level)
    }

    @Test
    fun `GIVEN configured getLogLevel WHEN called THEN uses custom behavior`() {
        val service: LoggingService = fakeLoggingService {
            getLogLevel { "DEBUG" }
        }

        val level = service.getLogLevel()

        assertEquals("DEBUG", level)
    }

    @Test
    fun `GIVEN all methods configured WHEN called THEN all use custom behaviors`() {
        var startCalled = false
        val messages = mutableListOf<String>()

        val service: LoggingService = fakeLoggingService {
            start {
                startCalled = true
                true
            }
            log { msg -> messages.add(msg) }
            getLogLevel { "TRACE" }
        }

        val started = service.start()
        service.log("msg1")
        val level = service.getLogLevel()

        assertTrue(startCalled)
        assertTrue(started)
        assertEquals(listOf("msg1"), messages)
        assertEquals("TRACE", level)
    }
}
