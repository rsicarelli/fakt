// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jlleitschuh.gradle.ktlint.KtlintExtension

/**
 * Ktlint convention for Fakt modules.
 *
 * Provides:
 * - Ktlint formatting and linting (Pinterest ktlint 1.7.1)
 * - Kotlin code style (not Android)
 * - Excludes generated code and build directories
 *
 * Tasks added:
 * - ktlintCheck: Verify code formatting
 * - ktlintFormat: Auto-format code
 */
internal fun Project.applyKtlintConvention() {
    pluginManager.apply("org.jlleitschuh.gradle.ktlint")

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
}
