// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

/**
 * Classifies Kotlin compilations as test or production code.
 *
 * **Purpose**: Determine if a compilation should generate fakes in test or main source sets.
 *
 * **Heuristics** (evaluated in order):
 * 1. **Standard test compilation name**: `compilation.name == "test"`
 * 2. **Convention**: `compilation.name.endsWith("Test", ignoreCase = true)`
 * 3. **Associated with main**: Compilation is associated with main compilation (test suite pattern)
 *
 * **Examples**:
 * - `test` → true (standard)
 * - `main` → false (standard)
 * - `integrationTest` → true (convention)
 * - `e2eTest` → true (convention)
 * - `debug` → false (Android variant, not test)
 * - `debugTest` → true (Android test variant)
 * - Custom suite associated with main → true (association pattern)
 *
 * @since 1.1.0
 */

internal object CompilationClassifier {
    /**
     * Determine if a compilation is for test code.
     *
     * @param compilation The compilation to classify
     * @return true if this is a test compilation, false if it's production code
     */
    fun isTestCompilation(compilation: KotlinCompilation<*>): Boolean {
        // Heuristic 1: Standard test compilation name
        if (compilation.name == KotlinCompilation.TEST_COMPILATION_NAME) {
            return true
        }

        // Heuristic 2: Convention - name ends with "Test" (case-insensitive)
        // Heuristic 3: Associated with main compilation (test suite pattern)
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        val associatedCompilations = compilation.allAssociatedCompilations
        return compilation.name.endsWith("Test", ignoreCase = true) ||
            associatedCompilations.any { it.name == KotlinCompilation.MAIN_COMPILATION_NAME }
    }
}
