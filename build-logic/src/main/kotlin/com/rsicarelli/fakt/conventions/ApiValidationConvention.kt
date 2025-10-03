// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.ExperimentalBCVApi
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Binary Compatibility Validator (API validation) convention.
 *
 * Configures:
 * - Ignored projects (compiler - internal implementation, samples - examples)
 * - Ignored packages (internal APIs)
 * - KLib validation for Kotlin Multiplatform
 */
fun Project.applyApiValidationConvention() {
    configure<ApiValidationExtension> {
        // Ignore internal implementation modules
        ignoredProjects += listOf("compiler")

        // Ignore all sample modules (not part of public API)
        ignoredProjects +=
            listOf(
                "single-module",
                "kmp-comprehensive-test",
                "api",
                "core",
                "app",
            )

        ignoredPackages += listOf("com.rsicarelli.fakt.internal")
        @OptIn(ExperimentalBCVApi::class)
        klib {
            enabled = true
        }
    }
}
