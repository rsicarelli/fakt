// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration

/**
 * Essential tests for KtFakeCommandLineProcessor focusing on core functionality.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtFakeCommandLineProcessorSimpleTest {

    @Test
    fun `GIVEN command line processor instance WHEN checking plugin ID THEN should have correct identifier`() = runTest {
        // Given - processor instance
        val processor = KtFakeCommandLineProcessor()

        // When - checking plugin ID
        val pluginId = processor.pluginId

        // Then - should have correct KtFake plugin identifier
        assertEquals("dev.rsicarelli.ktfake", pluginId, "Should have correct plugin ID")
    }

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `GIVEN processor instance WHEN checking inheritance THEN should extend CommandLineProcessor`() = runTest {
        // Given - processor instance
        val processor = KtFakeCommandLineProcessor()

        // When - checking class hierarchy
        val isCommandLineProcessor = processor is CommandLineProcessor

        // Then - should be a proper command line processor
        assertTrue(isCommandLineProcessor, "Should extend CommandLineProcessor")
        assertNotNull(processor, "Processor instance should not be null")
    }

    @Test
    fun `GIVEN processor instance WHEN getting plugin options THEN should provide all supported options`() = runTest {
        // Given - processor instance
        val processor = KtFakeCommandLineProcessor()

        // When - getting plugin options
        val options = processor.pluginOptions

        // Then - should have all expected options
        assertEquals(3, options.size, "Should have exactly 3 plugin options")

        val optionNames = options.map { it.optionName }.toSet()
        assertTrue(optionNames.contains("enabled"), "Should include 'enabled' option")
        assertTrue(optionNames.contains("debug"), "Should include 'debug' option")
        assertTrue(optionNames.contains("outputDir"), "Should include 'outputDir' option")
    }

    @Test
    fun `GIVEN configuration keys WHEN checking key definitions THEN should have proper types`() = runTest {
        // Given - configuration keys
        val enabledKey = KtFakeCommandLineProcessor.ENABLED_KEY
        val debugKey = KtFakeCommandLineProcessor.DEBUG_KEY
        val outputDirKey = KtFakeCommandLineProcessor.OUTPUT_DIR_KEY

        // When - examining key properties
        // Then - should have proper types and names
        assertNotNull(enabledKey, "Enabled key should be defined")
        assertNotNull(debugKey, "Debug key should be defined")
        assertNotNull(outputDirKey, "Output dir key should be defined")

        // Keys should have different identities
        assertTrue(enabledKey != debugKey, "Keys should be distinct")
        assertTrue(debugKey != outputDirKey, "Keys should be distinct")
        assertTrue(enabledKey != outputDirKey, "Keys should be distinct")
    }

    @Test
    fun `GIVEN empty configuration WHEN accessing unset options THEN should return null for missing values`() = runTest {
        // Given - empty configuration
        val configuration = CompilerConfiguration()

        // When - accessing unset configuration keys
        val enabledValue = configuration.get(KtFakeCommandLineProcessor.ENABLED_KEY)
        val debugValue = configuration.get(KtFakeCommandLineProcessor.DEBUG_KEY)
        val outputDirValue = configuration.get(KtFakeCommandLineProcessor.OUTPUT_DIR_KEY)

        // Then - should return null for unset values
        assertEquals(null, enabledValue, "Unset enabled value should be null")
        assertEquals(null, debugValue, "Unset debug value should be null")
        assertEquals(null, outputDirValue, "Unset output directory should be null")
    }

    @Test
    fun `GIVEN processor options WHEN checking all options are not required THEN should allow optional configuration`() = runTest {
        // Given - processor options
        val processor = KtFakeCommandLineProcessor()
        val options = processor.pluginOptions

        // When - checking required status of all options
        val requiredOptions = options.filter { it.required }
        val optionalOptions = options.filter { !it.required }

        // Then - all options should be optional
        assertEquals(0, requiredOptions.size, "No options should be required")
        assertEquals(3, optionalOptions.size, "All 3 options should be optional")
    }
}
