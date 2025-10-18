// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.SourceSetContext
import com.rsicarelli.fakt.gradle.helpers.SourceSetTestHelper
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import java.util.Base64
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * TDD tests for FaktGradleSubplugin SubpluginOption serialization.
 *
 * **Test Strategy**:
 * 1. Verify SubpluginOptions contain all required keys
 * 2. Verify [SourceSetContext] can be serialized to Base64+JSON
 * 3. Verify deserialization roundtrip (serialize → deserialize → verify)
 * 4. Verify context completeness (all source sets included)
 */
class FaktGradleSubpluginSerializationTest {
    @Test
    fun `GIVEN simple JVM context WHEN serializing THEN should create valid Base64 JSON`() {
        // GIVEN: Simple JVM test context (simulating what SourceSetDiscovery creates)
        val context =
            SourceSetTestHelper.createSimpleJvmTestContext(
                compilationName = "test",
                targetName = "jvm",
                outputDirectory = "/project/build/generated/fakt/test/kotlin",
            )

        // WHEN: Serializing to Base64 (same as Gradle plugin does)
        val json = Json { prettyPrint = false }
        val jsonString = json.encodeToString(SourceSetContext.serializer(), context)
        val base64Encoded = Base64.getEncoder().encodeToString(jsonString.toByteArray())

        // THEN: Should be valid Base64
        assertNotNull(base64Encoded)
        assertTrue(base64Encoded.isNotBlank(), "Base64 should not be blank")

        // THEN: Should be deserializable
        val decoded = String(Base64.getDecoder().decode(base64Encoded))
        val deserialized = json.decodeFromString(SourceSetContext.serializer(), decoded)

        assertEquals("test", deserialized.compilationName)
        assertEquals("jvm", deserialized.targetName)
        assertEquals("jvm", deserialized.platformType)
        assertTrue(deserialized.isTest)
    }

    @Test
    fun `GIVEN complex KMP context WHEN serializing THEN should preserve full hierarchy`() {
        // GIVEN: Complex iOS context (simulating KMP hierarchy)
        val context =
            SourceSetTestHelper.createIosKmpContext(
                compilationName = "main",
                targetName = "iosX64",
                outputDirectory = "/project/build/generated/fakt/main/kotlin",
            )

        // WHEN: Serializing and deserializing
        val json = Json { prettyPrint = false }
        val jsonString = json.encodeToString(SourceSetContext.serializer(), context)
        val base64Encoded = Base64.getEncoder().encodeToString(jsonString.toByteArray())

        val decoded = String(Base64.getDecoder().decode(base64Encoded))
        val deserialized = json.decodeFromString(SourceSetContext.serializer(), decoded)

        // THEN: Full hierarchy should be preserved
        assertEquals(5, deserialized.allSourceSets.size, "iOS hierarchy has 5 levels")
        val sourceSetNames = deserialized.allSourceSets.map { it.name }
        assertTrue(sourceSetNames.contains("iosX64Main"))
        assertTrue(sourceSetNames.contains("iosMain"))
        assertTrue(sourceSetNames.contains("appleMain"))
        assertTrue(sourceSetNames.contains("nativeMain"))
        assertTrue(sourceSetNames.contains("commonMain"))
    }

    @Test
    fun `GIVEN serialized context WHEN deserializing THEN should match original`() {
        // GIVEN: Original context
        val original =
            SourceSetTestHelper.createSimpleJvmTestContext(
                compilationName = "test",
                targetName = "jvm",
                outputDirectory = "/test/output",
            )

        // WHEN: Roundtrip serialization
        val json = Json { prettyPrint = false }
        val serialized = json.encodeToString(SourceSetContext.serializer(), original)
        val deserialized = json.decodeFromString(SourceSetContext.serializer(), serialized)

        // THEN: Should match exactly
        assertEquals(original.compilationName, deserialized.compilationName)
        assertEquals(original.targetName, deserialized.targetName)
        assertEquals(original.platformType, deserialized.platformType)
        assertEquals(original.isTest, deserialized.isTest)
        assertEquals(original.defaultSourceSet.name, deserialized.defaultSourceSet.name)
        assertEquals(original.allSourceSets.size, deserialized.allSourceSets.size)
        assertEquals(original.outputDirectory, deserialized.outputDirectory)
    }

    @Test
    fun `GIVEN context with multi-parent source sets WHEN serializing THEN should preserve all parents`() {
        // GIVEN: Context with multi-parent source set (e.g., appleTest → iosTest, macosTest)
        val context =
            SourceSetTestHelper.createMultiParentContext(
                compilationName = "test",
                targetName = "apple",
                outputDirectory = "/test/output",
            )

        // WHEN: Roundtrip
        val json = Json { prettyPrint = false }
        val serialized = json.encodeToString(SourceSetContext.serializer(), context)
        val deserialized = json.decodeFromString(SourceSetContext.serializer(), serialized)

        // THEN: Multi-parent relationships preserved
        val appleTest = deserialized.allSourceSets.first { it.name == "appleTest" }
        assertEquals(2, appleTest.parents.size, "appleTest should have 2 parents")
        assertTrue(appleTest.parents.contains("iosTest"))
        assertTrue(appleTest.parents.contains("macosTest"))
    }

    @Test
    fun `GIVEN context WHEN converting to JSON THEN should be compact (no pretty print)`() {
        // GIVEN: Context
        val context =
            SourceSetTestHelper.createSimpleJvmTestContext(
                compilationName = "test",
                targetName = "jvm",
                outputDirectory = "/test/output",
            )

        // WHEN: Serializing with prettyPrint = false (same as Gradle plugin)
        val json = Json { prettyPrint = false }
        val jsonString = json.encodeToString(SourceSetContext.serializer(), context)

        // THEN: Should be single line (no newlines)
        assertTrue(!jsonString.contains("\n"), "JSON should not contain newlines")
        assertTrue(
            jsonString.contains("\"compilationName\":\"test\""),
            "Should contain compilationName",
        )
        assertTrue(jsonString.contains("\"targetName\":\"jvm\""), "Should contain targetName")
    }

    @Test
    fun `GIVEN Base64 encoded context WHEN decoding THEN should be valid JSON`() {
        // GIVEN: Context serialized to Base64 (simulating SubpluginOption value)
        val context =
            SourceSetTestHelper.createSimpleJvmTestContext(
                compilationName = "test",
                targetName = "jvm",
                outputDirectory = "/test/output",
            )

        val json = Json { prettyPrint = false }
        val jsonString = json.encodeToString(SourceSetContext.serializer(), context)
        val base64Encoded = Base64.getEncoder().encodeToString(jsonString.toByteArray())

        // WHEN: Decoding Base64 (simulating what CommandLineProcessor does)
        val decoded = String(Base64.getDecoder().decode(base64Encoded))

        // THEN: Should be valid JSON
        assertTrue(decoded.startsWith("{"), "Should start with {")
        assertTrue(decoded.endsWith("}"), "Should end with }")
        assertTrue(decoded.contains("\"compilationName\""))
        assertTrue(decoded.contains("\"targetName\""))
        assertTrue(decoded.contains("\"platformType\""))
    }

    @Test
    fun `GIVEN context with empty parents WHEN serializing THEN should handle empty lists`() {
        // GIVEN: Context with commonMain (no parents)
        val context =
            SourceSetTestHelper.createSimpleJvmMainContext(
                compilationName = "main",
                targetName = "jvm",
                outputDirectory = "/main/output",
            )

        // WHEN: Roundtrip
        val json = Json { prettyPrint = false }
        val serialized = json.encodeToString(SourceSetContext.serializer(), context)
        val deserialized = json.decodeFromString(SourceSetContext.serializer(), serialized)

        // THEN: Empty parents should be preserved
        val commonMain = deserialized.allSourceSets.first { it.name == "commonMain" }
        assertEquals(0, commonMain.parents.size, "commonMain should have no parents")
    }
}
