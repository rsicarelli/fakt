// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.finalClasses.edgeCases

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggerTest {
    @Test
    fun `GIVEN unconfigured log WHEN called with varargs THEN uses super implementation`() {
        val logger: Logger = fakeLogger {}

        logger.log("msg1", "msg2", "msg3")

        // No exception - super implementation executes
    }

    @Test
    fun `GIVEN configured log WHEN called with varargs THEN uses custom behavior`() {
        val logged = mutableListOf<String>()

        val logger: Logger =
            fakeLogger {
                log { messages ->
                    logged.addAll(messages)
                }
            }

        logger.log("msg1", "msg2", "msg3")

        assertEquals(listOf("msg1", "msg2", "msg3"), logged)
    }

    @Test
    fun `GIVEN unconfigured logWithLevel WHEN called THEN uses super implementation`() {
        val logger: Logger = fakeLogger {}

        logger.logWithLevel("INFO", "msg1", "msg2")

        // No exception - super implementation executes
    }

    @Test
    fun `GIVEN configured logWithLevel WHEN called THEN uses custom behavior`() {
        var capturedLevel = ""
        val capturedMessages = mutableListOf<String>()

        val logger: Logger =
            fakeLogger {
                logWithLevel { level, messages ->
                    capturedLevel = level
                    capturedMessages.addAll(messages)
                }
            }

        logger.logWithLevel("ERROR", "msg1", "msg2")

        assertEquals("ERROR", capturedLevel)
        assertEquals(listOf("msg1", "msg2"), capturedMessages)
    }

    @Test
    fun `GIVEN unconfigured format WHEN called THEN uses super implementation`() {
        val logger: Logger = fakeLogger {}

        val result = logger.format("Hello %s", "World")

        assertEquals("Hello %s", result)
    }

    @Test
    fun `GIVEN configured format WHEN called THEN uses custom behavior`() {
        val logger: Logger =
            fakeLogger {
                format { template, args ->
                    var result = template
                    args.forEach { arg ->
                        result = result.replaceFirst("%s", arg.toString())
                    }
                    result
                }
            }

        val result = logger.format("Hello %s %s", "Beautiful", "World")

        assertEquals("Hello Beautiful World", result)
    }

    @Test
    fun `GIVEN unconfigured combine WHEN called THEN uses super implementation`() {
        val logger: Logger = fakeLogger {}

        val result = logger.combine("START-", "a", "b", "c", suffix = "-END")

        assertEquals("START-a, b, c-END", result)
    }

    @Test
    fun `GIVEN configured combine WHEN called THEN uses custom behavior`() {
        val logger: Logger =
            fakeLogger {
                combine { prefix, parts, suffix ->
                    "$prefix[${parts.joinToString(",")}]$suffix"
                }
            }

        val result = logger.combine("START-", "a", "b", "c", suffix = "-END")

        assertEquals("START-[a,b,c]-END", result)
    }

    @Test
    fun `GIVEN all methods configured WHEN called THEN all use custom behaviors`() {
        val allLogs = mutableListOf<String>()

        val logger: Logger =
            fakeLogger {
                log { messages ->
                    allLogs.add("log: ${messages.joinToString("")}")
                }
                logWithLevel { level, messages ->
                    allLogs.add("$level: ${messages.joinToString("")}")
                }
                format { template, args ->
                    "formatted: $template"
                }
                combine { prefix, parts, suffix ->
                    "$prefix-combined-$suffix"
                }
            }

        logger.log("a", "b")
        logger.logWithLevel("INFO", "c", "d")
        val formatted = logger.format("test", "arg")
        val combined = logger.combine("p", "x", suffix = "s")

        assertEquals(2, allLogs.size)
        assertEquals("log: ab", allLogs[0])
        assertEquals("INFO: cd", allLogs[1])
        assertEquals("formatted: test", formatted)
        assertEquals("p-combined-s", combined)
    }
}
