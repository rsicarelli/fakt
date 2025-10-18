// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import org.junit.jupiter.api.Disabled
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration test verifying end-to-end code generation.
 *
 * **NOTE**: This test requires samples/kmp-comprehensive-test to be compiled first.
 * Run `make test-sample` or `./gradlew :samples:kmp-comprehensive-test:build` before running this test.
 *
 * **DISABLED**: This test is disabled by default because it depends on external compilation.
 * Enable when running full integration test suite.
 */
class ExtractedModulesIntegrationTest {
    @Test
    @Disabled("Requires samples project to be compiled first - run 'make test-sample'")
    fun `GIVEN samples project WHEN compiled THEN should generate implementation files`() {
        assertTrue(
            compilationSucceeded(),
            "Code generation should produce Kotlin files",
        )
    }

    // Helper methods for integration testing

    private fun compilationSucceeded(): Boolean = findGeneratedKotlinFiles().isNotEmpty()

    private fun findGeneratedKotlinFiles(): List<File> {
        // After source set routing fix, generated files are in source set directories
        // e.g., commonTest/kotlin, jvmTest/kotlin, etc.
        val baseDir = File("../samples/kmp-comprehensive-test/build/generated/fakt")

        if (!baseDir.exists()) return emptyList()

        // Search all source set directories (commonTest, jvmTest, iosTest, etc.)
        return baseDir
            .walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    }
}
