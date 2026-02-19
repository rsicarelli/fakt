// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for JvmOnlyService fake generated in jvmTest.
 *
 * This test validates that:
 * 1. Fakt generates fakes for jvmMain interfaces in jvmTest
 * 2. The fake is ONLY available in JVM tests (not in other platforms)
 * 3. JVM-specific operations can be mocked
 */
class JvmOnlyServiceTest {
    @Test
    fun `GIVEN JvmOnlyService fake WHEN reading file THEN should return configured content`() {
        // Given
        val fake = fakeJvmOnlyService {
            readFile { path ->
                when (path) {
                    "/test.txt" -> "Test content"
                    else -> ""
                }
            }
        }

        // When
        val result = fake.readFile("/test.txt")

        // Then
        assertEquals("Test content", result)
        assertEquals(1, fake.readFileCalls.value.size)
    }

    @Test
    fun `GIVEN JvmOnlyService fake WHEN writing file THEN should return success`() {
        // Given
        val fake = fakeJvmOnlyService {
            writeFile { path, _ -> path.endsWith(".txt") }
        }

        // When
        val successResult = fake.writeFile("/output.txt", "content")
        val failureResult = fake.writeFile("/output.bin", "content")

        // Then
        assertTrue(successResult)
        assertFalse(failureResult)
        assertEquals(2, fake.writeFileCalls.value.size)
    }

    @Test
    fun `GIVEN JvmOnlyService fake WHEN getting system property THEN should return configured value`() {
        // Given
        val fake = fakeJvmOnlyService {
            getSystemProperty { key ->
                when (key) {
                    "java.version" -> "11.0.0"
                    else -> null
                }
            }
        }

        // When
        val javaVersion = fake.getSystemProperty("java.version")
        val unknownProp = fake.getSystemProperty("unknown")

        // Then
        assertEquals("11.0.0", javaVersion)
        assertNull(unknownProp)
        assertEquals(2, fake.getSystemPropertyCalls.value.size)
    }

    @Test
    fun `GIVEN JvmOnlyService fake WHEN accessing working directory THEN should return configured path`() {
        // Given
        val fake = fakeJvmOnlyService {
            workingDirectory { "/home/user/project" }
        }

        // When
        val result = fake.workingDirectory

        // Then
        assertEquals("/home/user/project", result)
        assertEquals(1, fake.workingDirectoryCalls.value.size)
    }

    @Test
    fun `GIVEN JvmOnlyService fake WHEN using defaults THEN should have sensible defaults`() {
        // Given
        val fake = fakeJvmOnlyService()

        // When
        val fileContent = fake.readFile("/any.txt")
        val writeResult = fake.writeFile("/any.txt", "content")
        val sysProp = fake.getSystemProperty("any.key")
        val workDir = fake.workingDirectory

        // Then
        assertEquals("/any.txt", fileContent) // Default: identity (returns first param)
        assertFalse(writeResult) // Default: false
        assertEquals("any.key", sysProp) // Default: identity (returns first param)
        assertEquals("", workDir) // Default: empty string (no params)
    }
}
