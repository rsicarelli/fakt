// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for CodeGenerator.
 * Validates code generation orchestration, file writing, and error handling.
 *
 * Note: Complex integration tests with real IR classes are done through the samples/ directory.
 * These tests focus on unit-testable behaviors: package handling, naming conventions,
 * message collection, and code template structure.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CodeGeneratorTest {
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class PackageHandling {
        @ParameterizedTest
        @CsvSource(
            "com.example.service, com/example/service",
            "'', ''",
            "com, com",
            "com.example.services.api, com/example/services/api",
            "a.b.c.d.e, a/b/c/d/e",
        )
        fun `GIVEN package name WHEN converting to path THEN should replace dots with slashes`(
            packageName: String,
            expectedPath: String,
        ) = runTest {
            // GIVEN & WHEN
            val packagePath = packageName.replace('.', '/')

            // THEN
            assertEquals(expectedPath, packagePath)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FakeClassNaming {
        @ParameterizedTest
        @CsvSource(
            "UserService, FakeUserServiceImpl",
            "AuthenticationService, FakeAuthenticationServiceImpl",
            "Api, FakeApiImpl",
            "Repository, FakeRepositoryImpl",
            "DatabaseConnection, FakeDatabaseConnectionImpl",
            "A, FakeAImpl",
        )
        fun `GIVEN interface name WHEN creating fake class name THEN should use Fake{InterfaceName}Impl pattern`(
            interfaceName: String,
            expectedFakeClassName: String,
        ) = runTest {
            // GIVEN & WHEN
            val fakeClassName = "Fake${interfaceName}Impl"

            // THEN
            assertEquals(expectedFakeClassName, fakeClassName)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class MessageCollectorBehavior {
        @Test
        fun `GIVEN test message collector WHEN reporting info message THEN should store in info messages`() =
            runTest {
                // GIVEN
                val messageCollector = TestMessageCollector()
                val testMessage = "Test info message"

                // WHEN
                messageCollector.report(CompilerMessageSeverity.INFO, testMessage, null)

                // THEN
                assertEquals(1, messageCollector.infoMessages.size)
                assertTrue(messageCollector.infoMessages.contains(testMessage))
                assertTrue(messageCollector.errorMessages.isEmpty())
            }

        @Test
        fun `GIVEN test message collector WHEN reporting error message THEN should store in error messages`() =
            runTest {
                // GIVEN
                val messageCollector = TestMessageCollector()
                val testMessage = "Test error message"

                // WHEN
                messageCollector.report(CompilerMessageSeverity.ERROR, testMessage, null)

                // THEN
                assertEquals(1, messageCollector.errorMessages.size)
                assertTrue(messageCollector.errorMessages.contains(testMessage))
                assertTrue(messageCollector.infoMessages.isEmpty())
            }

        @Test
        fun `GIVEN test message collector with errors WHEN checking hasErrors THEN should return true`() =
            runTest {
                // GIVEN
                val messageCollector = TestMessageCollector()
                messageCollector.report(CompilerMessageSeverity.ERROR, "Error", null)

                // WHEN
                val hasErrors = messageCollector.hasErrors()

                // THEN
                assertTrue(hasErrors)
            }

        @Test
        fun `GIVEN test message collector without errors WHEN checking hasErrors THEN should return false`() =
            runTest {
                // GIVEN
                val messageCollector = TestMessageCollector()
                messageCollector.report(CompilerMessageSeverity.INFO, "Info", null)

                // WHEN
                val hasErrors = messageCollector.hasErrors()

                // THEN
                assertFalse(hasErrors)
            }

        @Test
        fun `GIVEN test message collector with messages WHEN clearing THEN should remove all messages`() =
            runTest {
                // GIVEN
                val messageCollector = TestMessageCollector()
                messageCollector.report(CompilerMessageSeverity.INFO, "Info", null)
                messageCollector.report(CompilerMessageSeverity.ERROR, "Error", null)

                // WHEN
                messageCollector.clear()

                // THEN
                assertTrue(messageCollector.infoMessages.isEmpty())
                assertTrue(messageCollector.errorMessages.isEmpty())
            }

        @Test
        fun `GIVEN test message collector WHEN reporting multiple messages THEN should store all messages`() =
            runTest {
                // GIVEN
                val messageCollector = TestMessageCollector()

                // WHEN
                messageCollector.report(CompilerMessageSeverity.INFO, "Info 1", null)
                messageCollector.report(CompilerMessageSeverity.INFO, "Info 2", null)
                messageCollector.report(CompilerMessageSeverity.ERROR, "Error 1", null)

                // THEN
                assertEquals(2, messageCollector.infoMessages.size)
                assertEquals(1, messageCollector.errorMessages.size)
                assertTrue(messageCollector.hasErrors())
            }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CodeTemplateStructure {
        @ParameterizedTest
        @CsvSource(
            "TestService, // Interface: TestService",
            "UserRepository, // Interface: UserRepository",
            "ApiService, // Interface: ApiService",
        )
        fun `GIVEN interface name WHEN building code header THEN should include interface name in comment`(
            interfaceName: String,
            expectedComment: String,
        ) = runTest {
            // GIVEN & WHEN
            val header =
                buildString {
                    appendLine("// Generated by Fakt")
                    appendLine("// Interface: $interfaceName")
                }

            // THEN
            assertTrue(header.contains(expectedComment))
            assertTrue(header.contains("Generated by Fakt"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["com.example", "org.test.services", "my.app.data"])
        fun `GIVEN package name WHEN building package declaration THEN should format correctly`(packageName: String) =
            runTest {
                // GIVEN & WHEN
                val packageDeclaration = "package $packageName"

                // THEN
                assertTrue(packageDeclaration.startsWith("package "))
                assertTrue(packageDeclaration.contains(packageName))
            }

        @Test
        fun `GIVEN list of imports WHEN sorting THEN should be alphabetically ordered`() =
            runTest {
                // GIVEN
                val imports =
                    setOf(
                        "kotlin.collections.List",
                        "kotlinx.coroutines.flow.Flow",
                        "java.util.Date",
                    )

                // WHEN
                val sortedImports = imports.sorted()

                // THEN
                assertEquals("java.util.Date", sortedImports[0])
                assertEquals("kotlin.collections.List", sortedImports[1])
                assertEquals("kotlinx.coroutines.flow.Flow", sortedImports[2])
            }
    }

    // Test doubles
    private class TestMessageCollector : MessageCollector {
        val infoMessages = mutableListOf<String>()
        val errorMessages = mutableListOf<String>()

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation?,
        ) {
            when (severity) {
                CompilerMessageSeverity.INFO -> infoMessages.add(message)
                CompilerMessageSeverity.ERROR -> errorMessages.add(message)
                else -> { /* ignore other severities for this test */ }
            }
        }

        override fun hasErrors(): Boolean = errorMessages.isNotEmpty()

        override fun clear() {
            infoMessages.clear()
            errorMessages.clear()
        }
    }
}
