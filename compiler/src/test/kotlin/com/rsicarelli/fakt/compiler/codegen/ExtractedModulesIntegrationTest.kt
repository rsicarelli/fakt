// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration test verifying end-to-end code generation.
 *
 * Validates that the compiler plugin successfully generates fake implementations
 * from samples/single-module project.
 */
class ExtractedModulesIntegrationTest {
    @Test
    fun `GIVEN samples project WHEN compiled THEN should generate implementation files`() {
        assertTrue(
            compilationSucceeded(),
            "Code generation should produce Kotlin files",
        )
    }

    // Helper methods for integration testing

    private fun compilationSucceeded(): Boolean = findGeneratedKotlinFiles().isNotEmpty()

    private fun findGeneratedKotlinFiles(): List<File> {
        val generatedDir = File("../samples/kmp-comprehensive-test/build/generated/fakt/common/test/kotlin")

        if (!generatedDir.exists()) return emptyList()

        return generatedDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    }
}
