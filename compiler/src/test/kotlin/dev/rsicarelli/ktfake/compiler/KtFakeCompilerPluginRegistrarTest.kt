// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

/**
 * Tests for KtFakeCompilerPluginRegistrar registration and configuration.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover plugin registration, options loading, and extension registration.
 */
class KtFakeCompilerPluginRegistrarTest {

    private lateinit var registrar: KtFakeCompilerPluginRegistrar
    private lateinit var configuration: CompilerConfiguration
    private lateinit var testMessageCollector: TestMessageCollector

    @BeforeTest
    fun setUp() {
        registrar = KtFakeCompilerPluginRegistrar()
        configuration = CompilerConfiguration()
        testMessageCollector = TestMessageCollector()
        configuration.put(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, testMessageCollector)
    }

    @Test
    fun `GIVEN KtFakeCompilerPluginRegistrar WHEN checking K2 support THEN should support K2 compilation`() {
        // Given: KtFake compiler plugin registrar
        // When: Checking K2 support
        val supportsK2 = registrar.supportsK2

        // Then: Should support K2 compilation
        assertTrue(supportsK2, "KtFake compiler plugin should support K2 compilation")
    }

    @Test
    fun `GIVEN KtFakeOptions with default enabled configuration WHEN options loaded THEN should enable plugin`() {
        // Given: Default compiler configuration

        // When: Loading options from configuration
        val options = KtFakeOptions.load(configuration)

        // Then: Plugin should be enabled and debug mode enabled for development
        assertTrue(options.enabled, "Plugin should be enabled by default for development")
        assertTrue(options.debug, "Debug should be enabled by default for development")
    }

    @Test
    fun `GIVEN KtFakeOptions with default values WHEN loading configuration THEN should have expected defaults`() {
        // Given: Default compiler configuration

        // When: Loading KtFake options
        val options = KtFakeOptions.load(configuration)

        // Then: Should have expected default values for development
        assertTrue(options.enabled, "Plugin should be enabled by default for development")
        assertTrue(options.debug, "Debug should be enabled by default for development")
        assertTrue(options.generateCallTracking, "Call tracking generation should be enabled by default")
        assertTrue(options.generateBuilderPatterns, "Builder pattern generation should be enabled by default")
        assertTrue(!options.strictMode, "Strict mode should be disabled by default")
    }

    @Test
    fun `GIVEN KtFakeOptions WHEN converting to string THEN should contain all configuration properties`() {
        // Given: KtFake options with custom values
        val options = KtFakeOptions(
            enabled = true,
            debug = false,
            generateCallTracking = true,
            generateBuilderPatterns = false,
            strictMode = true
        )

        // When: Converting to string representation
        val optionsString = options.toString()

        // Then: Should contain all configuration properties
        assertTrue(optionsString.contains("enabled=true"), "String should contain enabled property")
        assertTrue(optionsString.contains("debug=false"), "String should contain debug property")
        assertTrue(optionsString.contains("generateCallTracking=true"), "String should contain generateCallTracking property")
        assertTrue(optionsString.contains("generateBuilderPatterns=false"), "String should contain generateBuilderPatterns property")
        assertTrue(optionsString.contains("strictMode=true"), "String should contain strictMode property")
    }

    // Test utilities following testing guidelines

    private class TestMessageCollector : MessageCollector {
        private val messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()

        override fun clear() {
            messages.clear()
        }

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation?
        ) {
            messages.add(severity to message)
        }

        override fun hasErrors(): Boolean = messages.any { it.first == CompilerMessageSeverity.ERROR }

        fun getMessages(severity: CompilerMessageSeverity): List<String> {
            return messages.filter { it.first == severity }.map { it.second }
        }

        fun getAllMessages(): List<Pair<CompilerMessageSeverity, String>> = messages.toList()
    }
}
