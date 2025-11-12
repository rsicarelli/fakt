// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.core.telemetry.UnifiedFakeMetrics
import com.rsicarelli.fakt.compiler.core.telemetry.UnifiedMetricsTree
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrGenerationTraceLoggingTest {

    @Test
    fun `GIVEN interfaces only WHEN logging trace THEN last interface closes with └─`() {
        // GIVEN
        val logger = TestLogger(LogLevel.DEBUG)
        val interfaceMetrics = listOf(
            UnifiedFakeMetrics(
                name = "UserService",
                firTimeNanos = 100,
                firTypeParamCount = 0,
                firMemberCount = 3,
                irTimeNanos = 200,
                irLOC = 50,
            ),
            UnifiedFakeMetrics(
                name = "Repository",
                firTimeNanos = 150,
                firTypeParamCount = 1,
                firMemberCount = 5,
                irTimeNanos = 300,
                irLOC = 80,
            ),
        )
        val classMetrics = emptyList<UnifiedFakeMetrics>()

        // WHEN
        logUnifiedTraceTestable(logger, interfaceMetrics, classMetrics)

        // THEN
        val output = logger.debugMessages.joinToString("\n")

        // Should show interfaces section
        assertTrue(output.contains("├─ Interfaces: 2"))

        // First interface should use ├─
        assertTrue(output.contains("│  ├─ UserService"))

        // Last interface should use └─ (closes the tree)
        assertTrue(output.contains("│  └─ Repository"))

        // Should show FIR analysis line
        assertTrue(output.contains("FIR analysis:"))

        // Should show IR generation line
        assertTrue(output.contains("IR generation:"))

        // Should NOT show Classes section
        assertTrue(!output.contains("Classes:"))
    }

    @Test
    fun `GIVEN classes only WHEN logging trace THEN shows Classes section only`() {
        // GIVEN
        val logger = TestLogger(LogLevel.DEBUG)
        val interfaceMetrics = emptyList<UnifiedFakeMetrics>()
        val classMetrics = listOf(
            UnifiedFakeMetrics(
                name = "KeyValueCache",
                firTimeNanos = 100,
                firTypeParamCount = 2,
                firMemberCount = 5,
                irTimeNanos = 200,
                irLOC = 72,
            ),
            UnifiedFakeMetrics(
                name = "FileRepository",
                firTimeNanos = 150,
                firTypeParamCount = 0,
                firMemberCount = 4,
                irTimeNanos = 300,
                irLOC = 59,
            ),
        )

        // WHEN
        logUnifiedTraceTestable(logger, interfaceMetrics, classMetrics)

        // THEN
        val output = logger.debugMessages.joinToString("\n")

        // Should show empty interfaces section
        assertTrue(output.contains("├─ Interfaces: 0"))

        // Should show Classes section with └─
        assertTrue(output.contains("└─ Classes: 2"))

        // First class should use ├─
        assertTrue(output.contains("   ├─ KeyValueCache"))

        // Last class should use └─
        assertTrue(output.contains("   └─ FileRepository"))
    }

    @Test
    fun `GIVEN both interfaces and classes WHEN logging trace THEN tree structure is correct`() {
        // GIVEN
        val logger = TestLogger(LogLevel.DEBUG)
        val interfaceMetrics = listOf(
            UnifiedFakeMetrics(
                name = "UserService",
                firTimeNanos = 100,
                firTypeParamCount = 0,
                firMemberCount = 3,
                irTimeNanos = 200,
                irLOC = 50,
            ),
            UnifiedFakeMetrics(
                name = "Repository",
                firTimeNanos = 150,
                firTypeParamCount = 1,
                firMemberCount = 5,
                irTimeNanos = 300,
                irLOC = 80,
            ),
            UnifiedFakeMetrics(
                name = "AsyncDataService",
                firTimeNanos = 200,
                firTypeParamCount = 0,
                firMemberCount = 3,
                irTimeNanos = 400,
                irLOC = 49,
            ),
        )
        val classMetrics = listOf(
            UnifiedFakeMetrics(
                name = "KeyValueCache",
                firTimeNanos = 100,
                firTypeParamCount = 2,
                firMemberCount = 5,
                irTimeNanos = 200,
                irLOC = 72,
            ),
            UnifiedFakeMetrics(
                name = "FileRepository",
                firTimeNanos = 150,
                firTypeParamCount = 0,
                firMemberCount = 4,
                irTimeNanos = 300,
                irLOC = 59,
            ),
        )

        // WHEN
        logUnifiedTraceTestable(logger, interfaceMetrics, classMetrics)

        // THEN
        val output = logger.debugMessages.joinToString("\n")

        // Should show interfaces section with ├─
        assertTrue(output.contains("├─ Interfaces: 3"))

        // All interfaces should use ├─ (none should close with └─)
        assertTrue(output.contains("│  ├─ UserService"))
        assertTrue(output.contains("│  ├─ Repository"))
        assertTrue(output.contains("│  ├─ AsyncDataService"))

        // Ensure no interface is closing the tree (would have "│  └─ InterfaceName" pattern)
        assertTrue(!output.contains("│  └─ UserService"))
        assertTrue(!output.contains("│  └─ Repository"))
        assertTrue(!output.contains("│  └─ AsyncDataService"))

        // Should show Classes section with └─ (closes the root)
        assertTrue(output.contains("└─ Classes: 2"))

        // First class should use ├─
        assertTrue(output.contains("   ├─ KeyValueCache"))

        // Last class should use └─
        assertTrue(output.contains("   └─ FileRepository"))
    }

    @Test
    fun `GIVEN interfaces with metadata WHEN logging trace THEN shows type parameters and members`() {
        // GIVEN
        val logger = TestLogger(LogLevel.DEBUG)
        val interfaceMetrics = listOf(
            UnifiedFakeMetrics(
                name = "DataCache",
                firTimeNanos = 100,
                firTypeParamCount = 1,
                firMemberCount = 6,
                irTimeNanos = 200,
                irLOC = 83,
            ),
        )
        val classMetrics = emptyList<UnifiedFakeMetrics>()

        // WHEN
        logUnifiedTraceTestable(logger, interfaceMetrics, classMetrics)

        // THEN
        val output = logger.debugMessages.joinToString("\n")

        // Should show all 3 lines for the interface
        assertTrue(output.contains("│  └─ DataCache"))
        assertTrue(output.contains("FIR analysis: 1 type parameters, 6 members"))
        assertTrue(output.contains("IR generation: FakeDataCacheImpl 83 LOC"))
    }

    /**
     * Testable version of logUnifiedTrace that accepts a TestLogger and uses UnifiedMetricsTree
     */
    private fun logUnifiedTraceTestable(
        logger: TestLogger,
        interfaceMetrics: List<UnifiedFakeMetrics>,
        classMetrics: List<UnifiedFakeMetrics>,
    ) {
        if (logger.logLevel < LogLevel.DEBUG) return

        val tree = UnifiedMetricsTree(
            interfaces = interfaceMetrics,
            classes = classMetrics
        )

        logger.debug(tree.toTreeString())
    }

    /**
     * Test message collector that captures messages for verification
     */
    private class TestMessageCollector : org.jetbrains.kotlin.cli.common.messages.MessageCollector {
        val messages = mutableListOf<String>()

        override fun clear() {
            messages.clear()
        }

        override fun hasErrors(): Boolean = false

        override fun report(
            severity: org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity,
            message: String,
            location: org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation?,
        ) {
            messages.add(message)
        }
    }

    /**
     * Test logger wrapper
     */
    private class TestLogger(logLevel: LogLevel) {
        private val collector = TestMessageCollector()
        private val logger = com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger(collector, logLevel)

        val debugMessages: List<String>
            get() = collector.messages

        val logLevel: LogLevel
            get() = logger.logLevel

        fun debug(message: String) = logger.debug(message)
    }
}
