// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler

import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Essential tests for KtFakeCompilerPluginRegistrar focusing on core functionality.
 */
@OptIn(ExperimentalCompilerApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KtFakeCompilerPluginRegistrarSimpleTest {

    @Test
    fun `GIVEN default registrar instance WHEN checking K2 support THEN should support K2 compiler`() = runTest {
        // Given - fresh registrar instance
        val registrar = KtFakeCompilerPluginRegistrar()

        // When - checking K2 support
        val supportsK2 = registrar.supportsK2

        // Then - should support K2 compiler
        assertTrue(supportsK2, "KtFake plugin should support K2 compiler")
    }

    @Test
    fun `GIVEN registrar instance WHEN checking plugin inheritance THEN should extend CompilerPluginRegistrar`() = runTest {
        // Given - registrar instance
        val registrar = KtFakeCompilerPluginRegistrar()

        // When - checking class hierarchy
        val isPluginRegistrar = registrar is CompilerPluginRegistrar

        // Then - should be a proper compiler plugin registrar
        assertTrue(isPluginRegistrar, "Should extend CompilerPluginRegistrar")
        assertNotNull(registrar, "Registrar instance should not be null")
    }

    @Test
    fun `GIVEN default options WHEN creating instance THEN should have correct defaults`() = runTest {
        // Given - default options instance
        val options = KtFakeOptions()

        // When - checking default values
        // Then - should have sensible defaults
        assertFalse(options.enabled, "Should be disabled by default")
        assertFalse(options.debug, "Should have debug disabled by default")
        assertTrue(options.generateCallTracking, "Should enable call tracking by default")
        assertTrue(options.generateBuilderPatterns, "Should enable builder patterns by default")
        assertFalse(options.strictMode, "Should have strict mode disabled by default")
        assertEquals(null, options.outputDir, "Should have null output dir by default")
    }

    @Test
    fun `GIVEN custom options WHEN creating instance THEN should preserve custom values`() = runTest {
        // Given - custom options configuration
        val customOptions = KtFakeOptions(
            enabled = true,
            debug = true,
            generateCallTracking = false,
            generateBuilderPatterns = false,
            strictMode = true,
            outputDir = "/custom/path"
        )

        // When - checking custom values
        // Then - should preserve all custom settings
        assertTrue(customOptions.enabled, "Should preserve enabled state")
        assertTrue(customOptions.debug, "Should preserve debug state")
        assertFalse(customOptions.generateCallTracking, "Should preserve call tracking setting")
        assertFalse(customOptions.generateBuilderPatterns, "Should preserve builder patterns setting")
        assertTrue(customOptions.strictMode, "Should preserve strict mode setting")
        assertEquals("/custom/path", customOptions.outputDir, "Should preserve output directory")
    }

    @Test
    fun `GIVEN options instance WHEN converting to string THEN should provide readable representation`() = runTest {
        // Given - options with mixed settings
        val options = KtFakeOptions(
            enabled = true,
            debug = false,
            generateCallTracking = true,
            generateBuilderPatterns = false,
            strictMode = true
        )

        // When - converting to string
        val stringRepresentation = options.toString()

        // Then - should contain all configuration values
        assertTrue(stringRepresentation.contains("enabled=true"), "Should show enabled state")
        assertTrue(stringRepresentation.contains("debug=false"), "Should show debug state")
        assertTrue(stringRepresentation.contains("generateCallTracking=true"), "Should show call tracking")
        assertTrue(stringRepresentation.contains("generateBuilderPatterns=false"), "Should show builder patterns")
        assertTrue(stringRepresentation.contains("strictMode=true"), "Should show strict mode")
    }

    @Test
    fun `GIVEN empty configuration WHEN loading options THEN should use defaults for missing keys`() = runTest {
        // Given - minimal configuration
        val configuration = CompilerConfiguration()
        // Not setting any KtFake-specific keys

        // When - loading options
        val options = KtFakeOptions.load(configuration)

        // Then - should handle missing keys gracefully with defaults
        // NOTE: KtFakeOptions.load() defaults to enabled=true, debug=true when keys are missing
        assertTrue(options.enabled, "Should default enabled to true when not specified")
        assertTrue(options.debug, "Should default debug to true when not specified")
        assertEquals(null, options.outputDir, "Should default output dir to null when not specified")
        assertTrue(options.generateCallTracking, "Should use default call tracking value")
        assertTrue(options.generateBuilderPatterns, "Should use default builder patterns value")
        assertFalse(options.strictMode, "Should use default strict mode value")
    }
}
