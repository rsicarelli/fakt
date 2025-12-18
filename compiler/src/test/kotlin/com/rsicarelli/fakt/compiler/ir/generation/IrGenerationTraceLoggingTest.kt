// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.core.telemetry.UnifiedFakeMetrics
import com.rsicarelli.fakt.compiler.core.telemetry.UnifiedMetricsTree
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertTrue

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IrGenerationTraceLoggingTest {
    @Test
    fun `GIVEN interfaces only WHEN logging trace THEN shows Generated section with proper branching`() {
        // GIVEN
        val logger = TestLogger(LogLevel.DEBUG)
        val interfaceMetrics =
            listOf(
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

        // Should show Generated section (new format combines interfaces + classes)
        assertTrue(output.contains("Generated: 2"), "Should show Generated: 2")

        // Should show fake names
        assertTrue(output.contains("UserService"), "Should show UserService")
        assertTrue(output.contains("Repository"), "Should show Repository")

        // Should show generated impl class names
        assertTrue(output.contains("FakeUserServiceImpl"), "Should show FakeUserServiceImpl")
        assertTrue(output.contains("FakeRepositoryImpl"), "Should show FakeRepositoryImpl")

        // Should show LOC
        assertTrue(output.contains("50 LOC"), "Should show 50 LOC")
        assertTrue(output.contains("80 LOC"), "Should show 80 LOC")
    }

    @Test
    fun `GIVEN classes only WHEN logging trace THEN shows Generated section with classes`() {
        // GIVEN
        val logger = TestLogger(LogLevel.DEBUG)
        val interfaceMetrics = emptyList<UnifiedFakeMetrics>()
        val classMetrics =
            listOf(
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

        // Should show Generated section (new format combines interfaces + classes)
        assertTrue(output.contains("Generated: 2"), "Should show Generated: 2")

        // Should show class names
        assertTrue(output.contains("KeyValueCache"), "Should show KeyValueCache")
        assertTrue(output.contains("FileRepository"), "Should show FileRepository")

        // Should show generated impl class names
        assertTrue(output.contains("FakeKeyValueCacheImpl"), "Should show FakeKeyValueCacheImpl")
        assertTrue(output.contains("FakeFileRepositoryImpl"), "Should show FakeFileRepositoryImpl")
    }

    @Test
    fun `GIVEN both interfaces and classes WHEN logging trace THEN tree structure is correct`() {
        // GIVEN
        val logger = TestLogger(LogLevel.DEBUG)
        val interfaceMetrics =
            listOf(
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
        val classMetrics =
            listOf(
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

        // Should show combined Generated section (3 interfaces + 2 classes = 5)
        assertTrue(output.contains("Generated: 5"), "Should show Generated: 5")

        // Should show all fakes in order (interfaces first, then classes)
        assertTrue(output.contains("UserService"), "Should show UserService")
        assertTrue(output.contains("Repository"), "Should show Repository")
        assertTrue(output.contains("AsyncDataService"), "Should show AsyncDataService")
        assertTrue(output.contains("KeyValueCache"), "Should show KeyValueCache")
        assertTrue(output.contains("FileRepository"), "Should show FileRepository")

        // Should show all fake impl names
        assertTrue(output.contains("FakeUserServiceImpl"), "Should show FakeUserServiceImpl")
        assertTrue(output.contains("FakeRepositoryImpl"), "Should show FakeRepositoryImpl")
        assertTrue(output.contains("FakeAsyncDataServiceImpl"), "Should show FakeAsyncDataServiceImpl")
        assertTrue(output.contains("FakeKeyValueCacheImpl"), "Should show FakeKeyValueCacheImpl")
        assertTrue(output.contains("FakeFileRepositoryImpl"), "Should show FakeFileRepositoryImpl")
    }

    @Test
    fun `GIVEN interfaces with metadata WHEN logging trace THEN shows LOC in output`() {
        // GIVEN
        val logger = TestLogger(LogLevel.DEBUG)
        val interfaceMetrics =
            listOf(
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

        // Should show Generated section
        assertTrue(output.contains("Generated: 1"), "Should show Generated: 1")

        // Should show fake name
        assertTrue(output.contains("DataCache"), "Should show DataCache")

        // Should show generated impl class with LOC
        assertTrue(output.contains("FakeDataCacheImpl"), "Should show FakeDataCacheImpl")
        assertTrue(output.contains("83 LOC"), "Should show 83 LOC")
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

        val tree =
            UnifiedMetricsTree(
                interfaces = interfaceMetrics,
                classes = classMetrics,
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
    private class TestLogger(
        logLevel: LogLevel,
    ) {
        private val collector = TestMessageCollector()
        private val logger =
            com.rsicarelli.fakt.compiler.core.telemetry
                .FaktLogger(collector, logLevel)

        val debugMessages: List<String>
            get() = collector.messages

        val logLevel: LogLevel
            get() = logger.logLevel

        fun debug(message: String) = logger.debug(message)
    }
}
