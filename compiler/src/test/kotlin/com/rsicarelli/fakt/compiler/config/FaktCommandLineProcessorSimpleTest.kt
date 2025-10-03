// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.config

import com.rsicarelli.fakt.compiler.config.FaktCommandLineProcessor
import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Essential tests for FaktCommandLineProcessor focusing on core functionality.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktCommandLineProcessorSimpleTest {
    @Test
    fun `GIVEN command line processor instance WHEN checking plugin ID THEN should have correct identifier`() =
        runTest {
            val processor = FaktCommandLineProcessor()
            val pluginId = processor.pluginId

            assertEquals("com.rsicarelli.fakt", pluginId, "Should have correct plugin ID")
        }

    @OptIn(ExperimentalCompilerApi::class)
    @Test
    fun `GIVEN processor instance WHEN checking inheritance THEN should extend CommandLineProcessor`() =
        runTest {
            val processor = FaktCommandLineProcessor()
            val isCommandLineProcessor = processor is CommandLineProcessor

            assertTrue(isCommandLineProcessor, "Should extend CommandLineProcessor")
            assertNotNull(processor, "Processor instance should not be null")
        }

    @Test
    fun `GIVEN processor instance WHEN getting plugin options THEN should provide all supported options`() =
        runTest {
            val processor = FaktCommandLineProcessor()
            val options = processor.pluginOptions

            assertEquals(3, options.size, "Should have exactly 3 plugin options")

            val optionNames = options.map { it.optionName }.toSet()
            assertTrue(optionNames.contains("enabled"), "Should include 'enabled' option")
            assertTrue(optionNames.contains("debug"), "Should include 'debug' option")
            assertTrue(optionNames.contains("outputDir"), "Should include 'outputDir' option")
        }

    @Test
    fun `GIVEN configuration keys WHEN checking key definitions THEN should have proper types`() =
        runTest {
            val enabledKey = FaktCommandLineProcessor.ENABLED_KEY
            val debugKey = FaktCommandLineProcessor.DEBUG_KEY
            val outputDirKey = FaktCommandLineProcessor.OUTPUT_DIR_KEY

            assertNotNull(enabledKey, "Enabled key should be defined")
            assertNotNull(debugKey, "Debug key should be defined")
            assertNotNull(outputDirKey, "Output dir key should be defined")

            assertTrue(enabledKey != debugKey, "Keys should be distinct")
            assertTrue(debugKey != outputDirKey, "Keys should be distinct")
            assertTrue(enabledKey != outputDirKey, "Keys should be distinct")
        }

    @Test
    fun `GIVEN empty configuration WHEN accessing unset options THEN should return null for missing values`() =
        runTest {
            val configuration = CompilerConfiguration()

            val enabledValue = configuration.get(FaktCommandLineProcessor.ENABLED_KEY)
            val debugValue = configuration.get(FaktCommandLineProcessor.DEBUG_KEY)
            val outputDirValue = configuration.get(FaktCommandLineProcessor.OUTPUT_DIR_KEY)

            assertEquals(null, enabledValue, "Unset enabled value should be null")
            assertEquals(null, debugValue, "Unset debug value should be null")
            assertEquals(null, outputDirValue, "Unset output directory should be null")
        }

    @Test
    fun `GIVEN processor options WHEN checking all options are not required THEN should allow optional configuration`() =
        runTest {
            val processor = FaktCommandLineProcessor()
            val options = processor.pluginOptions

            val requiredOptions = options.filter { it.required }
            val optionalOptions = options.filter { !it.required }

            assertEquals(0, requiredOptions.size, "No options should be required")
            assertEquals(3, optionalOptions.size, "All 3 options should be optional")
        }
}
