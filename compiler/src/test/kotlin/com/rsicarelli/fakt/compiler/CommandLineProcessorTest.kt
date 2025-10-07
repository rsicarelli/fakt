// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.compiler.api.SourceSetInfo
import com.rsicarelli.fakt.compiler.config.FaktCommandLineProcessor
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD tests for FaktCommandLineProcessor - deserializes SourceSetContext from Gradle plugin.
 *
 * **Architecture**:
 * ```
 * Gradle Plugin (serializes) → SubpluginOption → CommandLineProcessor (deserializes) → Compiler Plugin
 * ```
 *
 * **Test Strategy**:
 * 1. Test deserialization of simple contexts
 * 2. Test complex KMP hierarchies
 * 3. Test error handling for malformed input
 * 4. Test configuration object population
 */
class CommandLineProcessorTest {
    @Test
    fun `GIVEN valid serialized context WHEN processing options THEN should deserialize correctly`() =
        runTest {
            // GIVEN: Simple JVM test context
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

            val json = Json { prettyPrint = false }
            val jsonString = json.encodeToString(context)
            val base64Encoded = Base64.getEncoder().encodeToString(jsonString.toByteArray())

            // WHEN: Processing command line options
            val processor = FaktCommandLineProcessor()
            val configuration = CompilerConfiguration()

            processor.processOption(
                option = processor.pluginOptions.first { it.optionName == "sourceSetContext" } as AbstractCliOption,
                value = base64Encoded,
                configuration = configuration,
            )

            // THEN: Configuration should contain deserialized context
            val deserializedContext = configuration.get(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY)
            assertNotNull(deserializedContext, "Context should be deserialized")
            assertEquals("test", deserializedContext.compilationName)
            assertEquals("jvm", deserializedContext.targetName)
            assertEquals("jvm", deserializedContext.platformType)
            assertTrue(deserializedContext.isTest)
            assertEquals("jvmTest", deserializedContext.defaultSourceSet.name)
            assertEquals(2, deserializedContext.allSourceSets.size)
        }

    @Test
    fun `GIVEN complex KMP context WHEN processing THEN should preserve hierarchy`() =
        runTest {
            // GIVEN: iOS hierarchy - iosX64Main → iosMain → appleMain → nativeMain → commonMain
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

            val json = Json { prettyPrint = false }
            val jsonString = json.encodeToString(context)
            val base64Encoded = Base64.getEncoder().encodeToString(jsonString.toByteArray())

            // WHEN: Processing
            val processor = FaktCommandLineProcessor()
            val configuration = CompilerConfiguration()

            processor.processOption(
                option = processor.pluginOptions.first { it.optionName == "sourceSetContext" } as AbstractCliOption,
                value = base64Encoded,
                configuration = configuration,
            )

            // THEN: Full hierarchy preserved
            val deserializedContext = configuration.get(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY)
            assertNotNull(deserializedContext)
            assertEquals(5, deserializedContext.allSourceSets.size)
            assertEquals("iosX64Main", deserializedContext.defaultSourceSet.name)
            assertFalse(deserializedContext.isTest)

            // Verify parent relationships
            val iosX64Info = deserializedContext.allSourceSets.first { it.name == "iosX64Main" }
            assertTrue(iosX64Info.parents.contains("iosMain"))
        }

    @Test
    fun `GIVEN enabled option WHEN processing THEN should set enabled flag`() =
        runTest {
            // GIVEN
            val processor = FaktCommandLineProcessor()
            val configuration = CompilerConfiguration()

            // WHEN
            processor.processOption(
                option = processor.pluginOptions.first { it.optionName == "enabled" } as AbstractCliOption,
                value = "true",
                configuration = configuration,
            )

            // THEN
            val enabled = configuration.get(FaktCommandLineProcessor.ENABLED_KEY)
            assertTrue(enabled == true, "Enabled flag should be set")
        }

    @Test
    fun `GIVEN debug option WHEN processing THEN should set debug flag`() =
        runTest {
            // GIVEN
            val processor = FaktCommandLineProcessor()
            val configuration = CompilerConfiguration()

            // WHEN
            processor.processOption(
                option = processor.pluginOptions.first { it.optionName == "debug" } as AbstractCliOption,
                value = "true",
                configuration = configuration,
            )

            // THEN
            val debug = configuration.get(FaktCommandLineProcessor.DEBUG_KEY)
            assertTrue(debug == true, "Debug flag should be set")
        }

    @Test
    fun `GIVEN outputDir option WHEN processing THEN should set output directory`() =
        runTest {
            // GIVEN
            val processor = FaktCommandLineProcessor()
            val configuration = CompilerConfiguration()
            val outputPath = "/custom/output/path"

            // WHEN
            processor.processOption(
                option = processor.pluginOptions.first { it.optionName == "outputDir" } as AbstractCliOption,
                value = outputPath,
                configuration = configuration,
            )

            // THEN
            val outputDir = configuration.get(FaktCommandLineProcessor.OUTPUT_DIR_KEY)
            assertEquals(outputPath, outputDir, "Output directory should be set")
        }

    @Test
    fun `GIVEN invalid base64 WHEN processing sourceSetContext THEN should handle gracefully`() =
        runTest {
            // GIVEN: Invalid base64
            val processor = FaktCommandLineProcessor()
            val configuration = CompilerConfiguration()

            // WHEN/THEN: Should not crash, just leave context null
            processor.processOption(
                option = processor.pluginOptions.first { it.optionName == "sourceSetContext" } as AbstractCliOption,
                value = "invalid-base64!!!",
                configuration = configuration,
            )

            val context = configuration.get(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY)
            assertNull(context, "Context should be null on invalid input")
        }

    @Test
    fun `GIVEN malformed JSON WHEN processing sourceSetContext THEN should handle gracefully`() =
        runTest {
            // GIVEN: Valid base64 but invalid JSON
            val invalidJson = "{invalid json}"
            val base64Encoded = Base64.getEncoder().encodeToString(invalidJson.toByteArray())

            val processor = FaktCommandLineProcessor()
            val configuration = CompilerConfiguration()

            // WHEN/THEN: Should not crash
            processor.processOption(
                option = processor.pluginOptions.first { it.optionName == "sourceSetContext" } as AbstractCliOption,
                value = base64Encoded,
                configuration = configuration,
            )

            val context = configuration.get(FaktCommandLineProcessor.SOURCE_SET_CONTEXT_KEY)
            assertNull(context, "Context should be null on malformed JSON")
        }

    @Test
    fun `GIVEN processor WHEN getting plugin options THEN should declare all options`() =
        runTest {
            // GIVEN
            val processor = FaktCommandLineProcessor()

            // WHEN
            val options = processor.pluginOptions

            // THEN: Should have all required options
            val optionNames = options.map { it.optionName }
            assertTrue(optionNames.contains("enabled"), "Should have 'enabled' option")
            assertTrue(optionNames.contains("debug"), "Should have 'debug' option")
            assertTrue(optionNames.contains("outputDir"), "Should have 'outputDir' option")
            assertTrue(optionNames.contains("sourceSetContext"), "Should have 'sourceSetContext' option")
        }

    @Test
    fun `GIVEN processor WHEN getting plugin ID THEN should return correct ID`() =
        runTest {
            // GIVEN
            val processor = FaktCommandLineProcessor()

            // WHEN
            val pluginId = processor.pluginId

            // THEN
            assertEquals("com.rsicarelli.fakt", pluginId, "Plugin ID should match")
        }
}
