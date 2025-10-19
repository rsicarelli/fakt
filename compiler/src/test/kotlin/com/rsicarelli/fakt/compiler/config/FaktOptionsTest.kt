// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.config

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.config.CompilerConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD tests for FaktOptions - loading configuration from CompilerConfiguration.
 *
 * **Test Strategy**:
 * 1. Test sourceSetContext loading when present
 * 2. Test graceful handling when context is null
 * 3. Test all option loading (enabled, debug, outputDir)
 * 4. Test default values
 */
class FaktOptionsTest {
    @Test
    fun `GIVEN configuration with sourceSetContext WHEN loading options THEN should populate context`() =
        runTest {
            // GIVEN: Configuration with source set context
            val context =
                SourceSetContext(
                    compilationName = "test",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = true,
                    defaultSourceSet = SourceSetInfo("jvmTest", listOf("commonMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("jvmTest", listOf("commonMain")),
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/test/kotlin",
                )

            val configuration = CompilerConfiguration()
            configuration.put(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY, context)
            configuration.put(FaktCommandLineProcessor.ENABLED_KEY, true)
            configuration.put(FaktCommandLineProcessor.DEBUG_KEY, false)

            // WHEN: Loading options
            val options = FaktOptions.load(configuration)

            // THEN: Should load source set context
            assertNotNull(options.sourceSetContext, "Source set context should be loaded")
            assertEquals(context, options.sourceSetContext)
            assertEquals("test", options.sourceSetContext.compilationName)
            assertEquals("jvm", options.sourceSetContext.targetName)
            assertTrue(options.enabled)
            assertFalse(options.debug)
        }

    @Test
    fun `GIVEN configuration without sourceSetContext WHEN loading THEN should have null context`() =
        runTest {
            // GIVEN: Configuration without source set context
            val configuration = CompilerConfiguration()
            configuration.put(FaktCommandLineProcessor.ENABLED_KEY, true)
            configuration.put(FaktCommandLineProcessor.DEBUG_KEY, true)

            // WHEN: Loading options
            val options = FaktOptions.load(configuration)

            // THEN: Context should be null (backward compatibility)
            assertNull(
                options.sourceSetContext,
                "Source set context should be null when not provided"
            )
            assertTrue(options.enabled)
            assertTrue(options.debug)
        }

    @Test
    fun `GIVEN configuration with all options WHEN loading THEN should load all correctly`() =
        runTest {
            // GIVEN: Configuration with all options
            val context =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "iosX64",
                    platformType = "native",
                    isTest = false,
                    defaultSourceSet = SourceSetInfo("iosX64Main", listOf("iosMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("iosX64Main", listOf("iosMain")),
                            SourceSetInfo("iosMain", listOf("commonMain")),
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/main/kotlin",
                )

            val configuration = CompilerConfiguration()
            configuration.put(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY, context)
            configuration.put(FaktCommandLineProcessor.ENABLED_KEY, true)
            configuration.put(FaktCommandLineProcessor.DEBUG_KEY, true)
            configuration.put(FaktCommandLineProcessor.OUTPUT_DIR_KEY, "/custom/output")

            // WHEN: Loading options
            val options = FaktOptions.load(configuration)

            // THEN: All options should be loaded
            assertNotNull(options.sourceSetContext)
            assertEquals(context, options.sourceSetContext)
            assertTrue(options.enabled)
            assertTrue(options.debug)
            assertEquals("/custom/output", options.outputDir)
        }

    @Test
    fun `GIVEN empty configuration WHEN loading THEN should use default values`() =
        runTest {
            // GIVEN: Empty configuration
            val configuration = CompilerConfiguration()

            // WHEN: Loading options
            val options = FaktOptions.load(configuration)

            // THEN: Should use defaults
            assertNull(options.sourceSetContext, "Default context should be null")
            assertTrue(options.enabled, "Default enabled should be true")
            assertEquals(LogLevel.INFO, options.logLevel, "Default logLevel should be INFO")
            assertNull(options.outputDir, "Default outputDir should be null")
        }

    @Test
    fun `GIVEN complex KMP context WHEN loading THEN should preserve full hierarchy`() =
        runTest {
            // GIVEN: Complex KMP context with multiple source sets
            val context =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "iosX64",
                    platformType = "native",
                    isTest = false,
                    defaultSourceSet = SourceSetInfo("iosX64Main", listOf("iosMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("iosX64Main", listOf("iosMain")),
                            SourceSetInfo("iosMain", listOf("appleMain")),
                            SourceSetInfo("appleMain", listOf("nativeMain")),
                            SourceSetInfo("nativeMain", listOf("commonMain")),
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/main/kotlin",
                )

            val configuration = CompilerConfiguration()
            configuration.put(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY, context)

            // WHEN: Loading options
            val options = FaktOptions.load(configuration)

            // THEN: Full hierarchy should be preserved
            assertNotNull(options.sourceSetContext)
            assertEquals(5, options.sourceSetContext.allSourceSets.size)
            val sourceSetNames = options.sourceSetContext.allSourceSets.map { it.name }
            assertTrue(sourceSetNames.contains("iosX64Main"))
            assertTrue(sourceSetNames.contains("iosMain"))
            assertTrue(sourceSetNames.contains("appleMain"))
            assertTrue(sourceSetNames.contains("nativeMain"))
            assertTrue(sourceSetNames.contains("commonMain"))
        }

    @Test
    fun `GIVEN options with context WHEN converting to string THEN should include context info`() =
        runTest {
            // GIVEN: Options with context
            val context =
                SourceSetContext(
                    compilationName = "test",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = true,
                    defaultSourceSet = SourceSetInfo("jvmTest", listOf("commonMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo("jvmTest", listOf("commonMain")),
                            SourceSetInfo("commonMain", emptyList()),
                        ),
                    outputDirectory = "/project/build/generated/fakt/test/kotlin",
                )

            val configuration = CompilerConfiguration()
            configuration.put(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY, context)
            configuration.put(FaktCommandLineProcessor.ENABLED_KEY, true)
            configuration.put(FaktCommandLineProcessor.DEBUG_KEY, false)

            val options = FaktOptions.load(configuration)

            // WHEN: Converting to string
            val optionsString = options.toString()

            // THEN: String should contain context information
            assertTrue(optionsString.contains("enabled=true"))
            assertTrue(optionsString.contains("logLevel=INFO"))
            assertTrue(optionsString.contains("sourceSetContext"))
        }
}
