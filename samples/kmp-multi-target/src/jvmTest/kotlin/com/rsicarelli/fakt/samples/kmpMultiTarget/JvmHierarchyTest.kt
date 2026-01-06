// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests KMP source set hierarchy: jvmTest can access fakes from commonMain.
 *
 * Hierarchy: commonMain → jvmMain
 *
 * This test validates that:
 * 1. jvmTest can use FakeCommonPlatformServiceImpl (from commonMain)
 * 2. jvmTest can use FakeJvmOnlyServiceImpl (from jvmMain)
 * 3. Fakes from "parent" source sets are accessible in "child" tests
 */
class JvmHierarchyTest {
    @Test
    fun `GIVEN jvmTest WHEN using CommonPlatformService fake THEN should access fake from commonMain`() {
        // Given - Using fake from commonMain (parent source set)
        val commonFake = fakeCommonPlatformService {
            getPlatformName { "JVM via Common" }
        }

        // When
        val result = commonFake.getPlatformName()

        // Then
        assertEquals("JVM via Common", result)
        assertEquals(1, commonFake.getPlatformNameCallCount.value)
    }

    @Test
    fun `GIVEN jvmTest WHEN using JvmOnlyService fake THEN should access fake from jvmMain`() {
        // Given - Using fake from jvmMain (own source set)
        val jvmFake = fakeJvmOnlyService {
            getSystemProperty { key ->
                when (key) {
                    "java.version" -> "17.0.0"
                    else -> null
                }
            }
        }

        // When
        val result = jvmFake.getSystemProperty("java.version")

        // Then
        assertEquals("17.0.0", result)
        assertEquals(1, jvmFake.getSystemPropertyCallCount.value)
    }

    @Test
    fun `GIVEN jvmTest WHEN using both common and jvm fakes THEN should access both in hierarchy`() {
        // Given - Using fakes from both commonMain and jvmMain
        val commonFake = fakeCommonPlatformService {
            log { level, message -> println("[$level] $message") }
            getPlatformName { "JVM" }
        }
        val jvmFake = fakeJvmOnlyService {
            workingDirectory { "/home/user/project" }
        }

        // When
        commonFake.log("INFO", "Test from JVM")
        val platform = commonFake.getPlatformName()
        val workDir = jvmFake.workingDirectory

        // Then - Both fakes work correctly
        assertEquals("JVM", platform)
        assertEquals("/home/user/project", workDir)
        assertEquals(1, commonFake.logCallCount.value)
        assertEquals(1, commonFake.getPlatformNameCallCount.value)
        assertEquals(1, jvmFake.workingDirectoryCallCount.value)
    }
}
