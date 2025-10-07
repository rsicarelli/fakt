// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.api

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD tests for SourceSetContext serialization.
 * These tests define the expected behavior BEFORE implementation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SourceSetContextSerializationTest {
    private val json =
        Json {
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
                        SourceSetInfo(
                            name = "jvmMain",
                            parents = listOf("commonMain"),
                        ),
                    allSourceSets =
                        listOf(
                            SourceSetInfo(name = "jvmMain", parents = listOf("commonMain")),
                            SourceSetInfo(name = "commonMain", parents = emptyList()),
                        ),
                    outputDirectory = "/build/generated/fakt/main/jvm/kotlin",
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
                    defaultSourceSet = SourceSetInfo(name = "jvmTest", parents = listOf("commonTest")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo(name = "jvmTest", parents = listOf("commonTest")),
                            SourceSetInfo(name = "commonTest", parents = emptyList()),
                        ),
                    outputDirectory = "/build/generated/fakt/test/jvm/kotlin",
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
                        SourceSetInfo(
                            name = "iosX64Main",
                            parents = listOf("iosMain"),
                        ),
                    allSourceSets =
                        listOf(
                            SourceSetInfo(name = "iosX64Main", parents = listOf("iosMain")),
                            SourceSetInfo(name = "iosMain", parents = listOf("appleMain")),
                            SourceSetInfo(name = "appleMain", parents = listOf("nativeMain")),
                            SourceSetInfo(name = "nativeMain", parents = listOf("commonMain")),
                            SourceSetInfo(name = "commonMain", parents = emptyList()),
                        ),
                    outputDirectory = "/build/generated/fakt/main/iosX64/kotlin",
                )

            // WHEN
            val jsonString = json.encodeToString(original)
            val decoded = json.decodeFromString<SourceSetContext>(jsonString)

            // THEN
            assertEquals(5, decoded.allSourceSets.size, "Should have all 5 source sets in hierarchy")
            assertEquals("iosX64Main", decoded.defaultSourceSet.name)
            assertEquals("native", decoded.platformType)

            // Verify hierarchy preserved
            val iosX64Main = decoded.allSourceSets.find { it.name == "iosX64Main" }
            assertNotNull(iosX64Main)
            assertEquals(listOf("iosMain"), iosX64Main.parents)
        }

    @Test
    fun `GIVEN custom test suite WHEN serializing THEN should work correctly`() =
        runTest {
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
    fun `GIVEN empty parents list WHEN serializing THEN should handle correctly`() =
        runTest {
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
                    allSourceSets =
                        listOf(
                            SourceSetInfo(name = "commonMain", parents = emptyList()),
                        ),
                    outputDirectory = "/build/generated/fakt/main/metadata/kotlin",
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
    fun `GIVEN serialized JSON WHEN checking size THEN should be reasonable for command line`() =
        runTest {
            // GIVEN: Typical context
            val context =
                SourceSetContext(
                    compilationName = "main",
                    targetName = "jvm",
                    platformType = "jvm",
                    isTest = false,
                    defaultSourceSet = SourceSetInfo(name = "jvmMain", parents = listOf("commonMain")),
                    allSourceSets =
                        listOf(
                            SourceSetInfo(name = "jvmMain", parents = listOf("commonMain")),
                            SourceSetInfo(name = "commonMain", parents = emptyList()),
                        ),
                    outputDirectory = "/build/generated/fakt/main/jvm/kotlin",
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
