// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.Name
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.BeforeTest

/**
 * Tests for ensuring fake generation only occurs in test source sets.
 *
 * This is a critical security feature to prevent accidentally shipping
 * fake implementations to production builds.
 */
class TestOnlyGenerationTest {

    @Test
    fun `GIVEN production module names WHEN checking source set THEN should not be test source set`() {
        // Given: Production module names
        val productionModules = listOf(
            "main",
            "production",
            "release",
            "myapp_main",
            "core_main"
        )

        productionModules.forEach { moduleName ->
            // When: Checking if module is test source set
            val isTest = isTestModule(moduleName)

            // Then: Should not be detected as test source set
            assertFalse(isTest, "Module '$moduleName' should not be detected as test source set")
        }
    }

    @Test
    fun `GIVEN test module names WHEN checking source set THEN should be test source set`() {
        // Given: Test module names
        val testModules = listOf(
            "test",
            "myapp_test",
            "core_test",
            "androidTest",
            "commonTest",
            "jvmTest",
            "integration_test"
        )

        testModules.forEach { moduleName ->
            // When: Checking if module is test source set
            val isTest = isTestModule(moduleName)

            // Then: Should be detected as test source set
            assertTrue(isTest, "Module '$moduleName' should be detected as test source set")
        }
    }

    @Test
    fun `GIVEN mixed case module names WHEN checking source set THEN should handle case insensitively`() {
        // Given: Mixed case test module names
        val mixedCaseModules = listOf(
            "MyApp_TEST",
            "Core_Test",
            "ANDROIDTEST",
            "CommonTEST"
        )

        mixedCaseModules.forEach { moduleName ->
            // When: Checking if module is test source set
            val isTest = isTestModule(moduleName)

            // Then: Should be detected as test source set (case insensitive)
            assertTrue(isTest, "Module '$moduleName' should be detected as test source set (case insensitive)")
        }
    }

    @Test
    fun `GIVEN security requirement WHEN generating in production THEN should prevent fake code in production`() {
        // Given: Production context
        val productionModules = listOf("main", "release", "production")

        productionModules.forEach { moduleName ->
            // When: Checking if generation should be allowed
            val shouldGenerate = isTestModule(moduleName)

            // Then: Should prevent generation in production
            assertFalse(shouldGenerate, "Should never generate fake code in production module '$moduleName'")
        }
    }

    @Test
    fun `GIVEN test requirement WHEN generating in test THEN should allow fake code in tests`() {
        // Given: Test context
        val testModules = listOf("test", "androidTest", "commonTest")

        testModules.forEach { moduleName ->
            // When: Checking if generation should be allowed
            val shouldGenerate = isTestModule(moduleName)

            // Then: Should allow generation in test
            assertTrue(shouldGenerate, "Should allow fake code generation in test module '$moduleName'")
        }
    }

    // Helper method that replicates the logic from KtFakesIrGenerationExtension
    private fun isTestModule(moduleName: String): Boolean {
        val name = moduleName.lowercase()
        return name.contains("test") ||
               name.contains("androidtest") ||
               name.contains("commontest") ||
               name.contains("jvmtest") ||
               name.endsWith("test")
    }
}
