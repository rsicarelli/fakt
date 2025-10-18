// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Ktlint linting convention plugin.
 *
 * Applies ktlint linting to individual projects (replaces allprojects usage).
 * This plugin should be applied to each module that needs linting.
 *
 * Configuration Cache Friendly:
 * - No allprojects usage
 * - Declarative configuration
 * - Modular application
 */
plugins {
    id("org.jlleitschuh.gradle.ktlint")
}

configure<KtlintExtension> {
    // Use Pinterest ktlint version
    version.set("1.7.1")

    // Output configuration
    verbose.set(true)
    outputToConsole.set(true)

    // Code style: Kotlin (not Android)
    android.set(false)

    // Fail build on violations
    ignoreFailures.set(false)

    // Exclude generated code and build artifacts
    filter {
        exclude("**/generated/**")
        exclude("**/build/**")
    }
}
