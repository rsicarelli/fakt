// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.telemetry

import com.rsicarelli.fakt.compiler.api.LogLevel
import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Simple MessageCollector implementation for testing.
 */
private class TestMessageCollector : MessageCollector {
    val messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()

    override fun clear() {
        messages.clear()
    }

    override fun hasErrors(): Boolean = messages.any { it.first.isError }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?,
    ) {
        messages.add(severity to message)
    }
}

/**
 * Tests for FaktLogger - level-aware logging wrapper.
 *
 * Tests follow GIVEN-WHEN-THEN pattern and use vanilla JUnit5 + kotlin-test.
 */
class FaktLoggerTest {
    @Test
    fun `GIVEN QUIET level WHEN logging info THEN should not log`() =
        runTest {
            // GIVEN: Test message collector
            val messageCollector = TestMessageCollector()
            val logger = FaktLogger(messageCollector, LogLevel.QUIET)

            // WHEN: Attempting to log at INFO level
            logger.info("Test info message")

            // THEN: Should not log anything
            assertTrue(messageCollector.messages.isEmpty(), "QUIET level should not log INFO messages")
        }

    @Test
    fun `GIVEN INFO level WHEN logging info THEN should log message`() =
        runTest {
            // GIVEN: Test message collector
            val messageCollector = TestMessageCollector()
            val logger = FaktLogger(messageCollector, LogLevel.INFO)

            // WHEN: Logging at INFO level
            logger.info("Processing interface")

            // THEN: Should log the message
            assertEquals(1, messageCollector.messages.size, "Should log 1 message")
            assertEquals(CompilerMessageSeverity.INFO, messageCollector.messages[0].first)
            assertTrue(messageCollector.messages[0].second.contains("Processing interface"))
            assertTrue(messageCollector.messages[0].second.startsWith("Fakt:"))
        }

    @Test
    fun `GIVEN INFO level WHEN logging debug THEN should not log`() =
        runTest {
            // GIVEN: Test message collector at INFO level
            val messageCollector = TestMessageCollector()
            val logger = FaktLogger(messageCollector, LogLevel.INFO)

            // WHEN: Attempting to log at DEBUG level
            logger.debug("Debug details")

            // THEN: Should not log (INFO < DEBUG)
            assertTrue(messageCollector.messages.isEmpty(), "INFO level should not log DEBUG messages")
        }

    @Test
    fun `GIVEN DEBUG level WHEN logging info and debug THEN should log both`() =
        runTest {
            // GIVEN: Test message collector at DEBUG level
            val messageCollector = TestMessageCollector()
            val logger = FaktLogger(messageCollector, LogLevel.DEBUG)

            // WHEN: Logging at INFO and DEBUG levels
            logger.info("Info message")
            logger.debug("Debug message")

            // THEN: Should log both messages
            assertEquals(2, messageCollector.messages.size, "DEBUG level should log both INFO and DEBUG")
            assertTrue(messageCollector.messages[0].second.contains("Info message"))
            assertTrue(messageCollector.messages[1].second.contains("Debug message"))
        }

    @Test
    fun `GIVEN DEBUG level WHEN logging trace THEN should not log`() =
        runTest {
            // GIVEN: Test message collector at DEBUG level
            val messageCollector = TestMessageCollector()
            val logger = FaktLogger(messageCollector, LogLevel.DEBUG)

            // WHEN: Attempting to log at TRACE level
            logger.trace("Trace details")

            // THEN: Should not log (DEBUG < TRACE)
            assertTrue(messageCollector.messages.isEmpty(), "DEBUG level should not log TRACE messages")
        }

    @Test
    fun `GIVEN TRACE level WHEN logging all levels THEN should log everything`() =
        runTest {
            // GIVEN: Test message collector at TRACE level
            val messageCollector = TestMessageCollector()
            val logger = FaktLogger(messageCollector, LogLevel.TRACE)

            // WHEN: Logging at all levels
            logger.info("Info message")
            logger.debug("Debug message")
            logger.trace("Trace message")

            // THEN: Should log all 3 messages
            assertEquals(3, messageCollector.messages.size, "TRACE level should log everything")
            assertTrue(messageCollector.messages[0].second.contains("Info message"))
            assertTrue(messageCollector.messages[1].second.contains("Debug message"))
            assertTrue(messageCollector.messages[2].second.contains("Trace message"))
        }

    @Test
    fun `GIVEN any level WHEN logging warning THEN should always log`() =
        runTest {
            // GIVEN: Test message collector at QUIET level (most restrictive)
            val messageCollector = TestMessageCollector()
            val logger = FaktLogger(messageCollector, LogLevel.QUIET)

            // WHEN: Logging a warning
            logger.warn("Warning message")

            // THEN: Should log the warning (warnings always shown)
            assertEquals(1, messageCollector.messages.size, "Warnings should always be logged")
            assertEquals(CompilerMessageSeverity.WARNING, messageCollector.messages[0].first)
            assertTrue(messageCollector.messages[0].second.contains("Warning message"))
        }

    @Test
    fun `GIVEN any level WHEN logging error THEN should always log`() =
        runTest {
            // GIVEN: Test message collector at QUIET level (most restrictive)
            val messageCollector = TestMessageCollector()
            val logger = FaktLogger(messageCollector, LogLevel.QUIET)

            // WHEN: Logging an error
            logger.error("Error message")

            // THEN: Should log the error (errors always shown)
            assertEquals(1, messageCollector.messages.size, "Errors should always be logged")
            assertEquals(CompilerMessageSeverity.ERROR, messageCollector.messages[0].first)
            assertTrue(messageCollector.messages[0].second.contains("Error message"))
        }

    @Test
    fun `GIVEN null messageCollector WHEN logging THEN should not crash`() =
        runTest {
            // GIVEN: Logger with null messageCollector
            val logger = FaktLogger(null, LogLevel.INFO)

            // WHEN: Attempting to log
            logger.info("Test message")
            logger.debug("Debug message")
            logger.warn("Warning")
            logger.error("Error")

            // THEN: Should not crash (null-safe)
            // Test passes if no exception thrown
        }

    @Test
    fun `GIVEN factory methods WHEN creating loggers THEN should create with correct levels`() =
        runTest {
            // GIVEN: Using factory methods
            val quietLogger = FaktLogger.quiet()
            val infoLogger = FaktLogger.info(null)
            val debugLogger = FaktLogger.debug(null)
            val traceLogger = FaktLogger.trace(null)

            // WHEN: Creating loggers via factories
            // THEN: Loggers should have correct levels (verified by trying to log)
            val collector = TestMessageCollector()

            // Test quiet - should not log info
            FaktLogger.quiet(collector).info("test")
            assertEquals(0, collector.messages.size)

            // Test info - should log info
            collector.clear()
            FaktLogger.info(collector).info("test")
            assertEquals(1, collector.messages.size)

            // Test debug - should log debug
            collector.clear()
            FaktLogger.debug(collector).debug("test")
            assertEquals(1, collector.messages.size)

            // Test trace - should log trace
            collector.clear()
            FaktLogger.trace(collector).trace("test")
            assertEquals(1, collector.messages.size)
        }

    @Test
    fun `GIVEN ifLevel block WHEN level matches THEN should execute block`() =
        runTest {
            // GIVEN: Logger at DEBUG level
            val logger = FaktLogger(null, LogLevel.DEBUG)
            var blockExecuted = false

            // WHEN: Using ifLevel with DEBUG threshold
            logger.ifLevel(LogLevel.DEBUG) {
                blockExecuted = true
            }

            // THEN: Block should be executed
            assertTrue(blockExecuted, "Block should execute when level >= threshold")
        }

    @Test
    fun `GIVEN ifLevel block WHEN level does not match THEN should not execute block`() =
        runTest {
            // GIVEN: Logger at INFO level
            val logger = FaktLogger(null, LogLevel.INFO)
            var blockExecuted = false

            // WHEN: Using ifLevel with DEBUG threshold
            logger.ifLevel(LogLevel.DEBUG) {
                blockExecuted = true
            }

            // THEN: Block should not be executed
            assertTrue(!blockExecuted, "Block should not execute when level < threshold")
        }
}
