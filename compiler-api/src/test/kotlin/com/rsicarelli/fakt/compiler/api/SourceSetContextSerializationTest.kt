// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.TestInstance

/**
 * TDD tests for SourceSetContext serialization. These tests define the expected behavior BEFORE
 * implementation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SourceSetContextSerializationTest {
    private val json = Json {
        prettyPrint = false
        encodeDefaults = true
    }

    @Test
    fun `GIVEN simple SourceSetContext WHEN serializing THEN should roundtrip perfectly`() =
        runTest {
            // GIVEN
            val original =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = false,
                    defaultSourceSet =
                        SourceSetInfo(name = "jvmMain", parents = listOf("commonMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo(name = "jvmMain", parents = listOf("commonMain")),
                            SourceSetInfo(name = "commonMain", parents = emptyList()),
                        ),
                    outputDirectory = "/build/generated/fakt/main/jvm/kotlin",
                    commonTestOutputDirectory = "/build/generated/fakt/commonTest/kotlin",
                )

            // WHEN
            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<SourceSetContext>(jsonString)

            // THEN
            assertEquals(original, decoded)
        }

    @Test
    fun `GIVEN test compilation context WHEN serializing THEN should preserve isTest flag`() =
        runTest {
            // GIVEN
            val original =
                SourceSetContext(
                    compilationName = "test",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = true, // Key: test compilation
                    defaultSourceSet =
                        SourceSetInfo(name = "jvmTest", parents = listOf("commonTest")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo(name = "jvmTest", parents = listOf("commonTest")),
                            SourceSetInfo(name = "commonTest", parents = emptyList()),
                        ),
                    outputDirectory = "/build/generated/fakt/test/jvm/kotlin",
                    commonTestOutputDirectory = "/build/generated/fakt/commonTest/kotlin",
                )

            // WHEN
            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<SourceSetContext>(jsonString)

            // THEN
            assertTrue(decoded.isTest, "isTest flag should be true")
            assertEquals("test", decoded.compilationName)
        }

    @Test
    fun `GIVEN complex KMP hierarchy WHEN serializing THEN should preserve all source sets`() =
        runTest {
            // GIVEN: iOS hierarchy
            // iosX64Main → iosMain → appleMain → nativeMain → commonMain
            val original =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "iosX64",
                    platformType = "native",
                    isTest = false,
                    defaultSourceSet =
                        SourceSetInfo(name = "iosX64Main", parents = listOf("iosMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo(name = "iosX64Main", parents = listOf("iosMain")),
                            SourceSetInfo(name = "iosMain", parents = listOf("appleMain")),
                            SourceSetInfo(name = "appleMain", parents = listOf("nativeMain")),
                            SourceSetInfo(name = "nativeMain", parents = listOf("commonMain")),
                            SourceSetInfo(name = "commonMain", parents = emptyList()),
                        ),
                    outputDirectory = "/build/generated/fakt/main/iosX64/kotlin",
                    commonTestOutputDirectory = "/build/generated/fakt/commonTest/kotlin",
                )

            // WHEN
            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<SourceSetContext>(jsonString)

            // THEN
            assertEquals(
                5,
                decoded.allSourceSets.size,
                "Should have all 5 source sets in hierarchy",
            )
            assertEquals("iosX64Main", decoded.defaultSourceSet.name)
            assertEquals("native", decoded.platformType)

            // Verify hierarchy preserved
            val iosX64Main = decoded.allSourceSets.find { it.name == "iosX64Main" }
            assertNotNull(iosX64Main)
            assertEquals(listOf("iosMain"), iosX64Main.parents)
        }

    @Test
    fun `GIVEN custom test suite WHEN serializing THEN should work correctly`() = runTest {
        // GIVEN: integrationTest compilation
        val original =
            SourceSetContext(
                compilationName = "integrationTest",
                targetName = "jvm",
                platformType = "jvm",
                isTest = true,
                defaultSourceSet =
                    SourceSetInfo(
                        name = "integrationTest",
                        parents = listOf("commonMain"), // Custom test suite depends on main
                    ),
                allSourceSets =
                    listOf(
                        SourceSetInfo(name = "integrationTest", parents = listOf("commonMain")),
                        SourceSetInfo(name = "commonMain", parents = emptyList()),
                    ),
                outputDirectory = "/build/generated/fakt/test/jvm/kotlin",
                commonTestOutputDirectory = "/build/generated/fakt/commonTest/kotlin",
            )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<SourceSetContext>(jsonString)

        // THEN
        assertEquals("integrationTest", decoded.compilationName)
        assertTrue(decoded.isTest)
        assertEquals(2, decoded.allSourceSets.size)
    }

    @Test
    fun `GIVEN empty parents list WHEN serializing THEN should handle correctly`() = runTest {
        // GIVEN: commonMain has no parents (root of hierarchy)
        val original =
            SourceSetContext(
                compilationName = "main",
                targetName = "metadata",
                platformType = "common",
                isTest = false,
                defaultSourceSet =
                    SourceSetInfo(
                        name = "commonMain",
                        parents = emptyList(), // Root has no parents
                    ),
                allSourceSets = listOf(SourceSetInfo(name = "commonMain", parents = emptyList())),
                outputDirectory = "/build/generated/fakt/main/metadata/kotlin",
                commonTestOutputDirectory = "/build/generated/fakt/commonTest/kotlin",
            )

        // WHEN
        val jsonString = json.encodeToString(original)
        val decoded = json.decodeFromString<SourceSetContext>(jsonString)

        // THEN
        assertTrue(decoded.defaultSourceSet.parents.isEmpty(), "Root should have no parents")
        assertEquals("metadata", decoded.targetName)
        assertEquals("common", decoded.platformType)
    }

    @Test
    fun `GIVEN legacy JSON without emitPhase WHEN deserializing THEN emitPhase defaults to IR`() =
        runTest {
            // GIVEN: a payload produced before the emitPhase field existed
            val legacyJson =
                """{"compilationName":"commonMain","targetName":"metadata",""" +
                    """"platformType":"common","isTest":false,""" +
                    """"defaultSourceSet":{"name":"commonMain","parents":[]},""" +
                    """"allSourceSets":[{"name":"commonMain","parents":[]}],""" +
                    """"outputDirectory":"/build/generated/fakt/commonTest/kotlin",""" +
                    """"commonTestOutputDirectory":"/build/generated/fakt/commonTest/kotlin"}"""

            // WHEN
            val decoded = Json.decodeFromString<SourceSetContext>(legacyJson)

            // THEN
            assertEquals(EmitPhase.IR, decoded.emitPhase)
        }

    @Test
    fun `GIVEN default emitPhase WHEN encoding with defaults omitted THEN field is absent from JSON`() =
        runTest {
            // GIVEN: the worker and wiring encode with plain Json (encodeDefaults = false)
            val defaultOmittingJson = Json
            val context =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = false,
                    defaultSourceSet = SourceSetInfo(name = "jvmMain", parents = emptyList()),
                    allSourceSets = listOf(SourceSetInfo(name = "jvmMain", parents = emptyList())),
                    outputDirectory = "/build/generated/fakt/main/jvm/kotlin",
                    commonTestOutputDirectory = "/build/generated/fakt/commonTest/kotlin",
                )

            // WHEN
            val jsonString = defaultOmittingJson.encodeToString(context)

            // THEN: legacy @Input payloads stay byte-identical after the field was added
            assertTrue(
                "emitPhase" !in jsonString,
                "Default emitPhase must be omitted to keep legacy payloads byte-identical. " +
                    "Got: $jsonString",
            )
        }

    @Test
    fun `GIVEN default emitSourceSets WHEN encoding with defaults omitted THEN field is absent from JSON`() =
        runTest {
            // GIVEN: the worker and wiring encode with plain Json (encodeDefaults = false)
            val context =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = false,
                    defaultSourceSet = SourceSetInfo(name = "jvmMain", parents = emptyList()),
                    allSourceSets = listOf(SourceSetInfo(name = "jvmMain", parents = emptyList())),
                    outputDirectory = "/build/generated/fakt/main/jvm/kotlin",
                    commonTestOutputDirectory = "/build/generated/fakt/commonTest/kotlin",
                )

            // WHEN
            val jsonString = Json.encodeToString(context)

            // THEN: legacy @Input payloads stay byte-identical after the field was added
            assertTrue(
                "emitSourceSets" !in jsonString,
                "Default emitSourceSets must be omitted to keep legacy payloads byte-identical. " +
                    "Got: $jsonString",
            )
        }

    @Test
    fun `GIVEN emitSourceSets restriction WHEN roundtripping THEN restriction is preserved`() =
        runTest {
            // GIVEN: a consumer invocation restricted to its own source set
            val original =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = false,
                    defaultSourceSet =
                        SourceSetInfo(name = "jvmMain", parents = listOf("commonMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo(name = "jvmMain", parents = listOf("commonMain")),
                            SourceSetInfo(name = "commonMain", parents = emptyList()),
                        ),
                    outputDirectory = "/build/generated/fakt/jvm/main/kotlin",
                    commonTestOutputDirectory = "/build/generated/fakt/commonTest/kotlin",
                    emitPhase = EmitPhase.FIR,
                    emitSourceSets = listOf("jvmMain"),
                )

            // WHEN
            val decoded = Json.decodeFromString<SourceSetContext>(Json.encodeToString(original))

            // THEN
            assertEquals(listOf("jvmMain"), decoded.emitSourceSets)
            assertEquals(original, decoded)
        }

    @Test
    fun `GIVEN FIR emitPhase WHEN roundtripping THEN emitPhase is preserved`() = runTest {
        // GIVEN
        val original =
            SourceSetContext(
                compilationName = "commonMain",
                targetName = "metadata",
                platformType = "common",
                isTest = false,
                defaultSourceSet = SourceSetInfo(name = "commonMain", parents = emptyList()),
                allSourceSets = listOf(SourceSetInfo(name = "commonMain", parents = emptyList())),
                outputDirectory = "/build/generated/fakt/commonTest/kotlin",
                commonTestOutputDirectory = "/build/generated/fakt/commonTest/kotlin",
                metadataOutputPath = "/build/generated/fakt/metadata/fir-metadata.json",
                emitPhase = EmitPhase.FIR,
            )

        // WHEN
        val jsonString = Json.encodeToString(original)
        val decoded = Json.decodeFromString<SourceSetContext>(jsonString)

        // THEN
        assertEquals(EmitPhase.FIR, decoded.emitPhase)
        assertEquals(original, decoded)
    }

    @Test
    fun `GIVEN serialized JSON WHEN checking size THEN should be reasonable for command line`() =
        runTest {
            // GIVEN: Typical context
            val context =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = false,
                    defaultSourceSet =
                        SourceSetInfo(name = "jvmMain", parents = listOf("commonMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo(name = "jvmMain", parents = listOf("commonMain")),
                            SourceSetInfo(name = "commonMain", parents = emptyList()),
                        ),
                    outputDirectory = "/build/generated/fakt/main/jvm/kotlin",
                    commonTestOutputDirectory = "/build/generated/fakt/commonTest/kotlin",
                )

            // WHEN
            val jsonString = json.encodeToString(context)

            // THEN
            assertTrue(
                jsonString.length < 5000,
                "Serialized context should be < 5KB for typical case. Got: ${jsonString.length} bytes",
            )
        }
}
