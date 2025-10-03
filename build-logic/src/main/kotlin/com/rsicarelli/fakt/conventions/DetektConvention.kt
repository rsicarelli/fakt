// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Detekt static analysis convention for all projects in the build.
 *
 * Applies Detekt static code analysis to all projects with:
 * - Detekt 1.23.8
 * - Default config as baseline
 * - Parallel execution for performance
 * - HTML reports enabled
 * - Excludes generated code and build directories
 *
 * Tasks added to all projects:
 * - detekt: Run static analysis
 * - detektBaseline: Generate baseline for existing issues
 */
fun Project.applyDetektToAllProjects() {
    allprojects {
        pluginManager.apply("io.gitlab.arturbosch.detekt")

        configure<DetektExtension> {
            // Build upon default config
            buildUponDefaultConfig = true

            // Fail build on violations
            ignoreFailures = false

            // Enable parallel execution
            parallel = true

            // Source directories
            source.setFrom("src/main/java", "src/main/kotlin")
        }

        tasks.withType<Detekt>().configureEach {
            // Exclude generated code and build artifacts
            exclude("**/generated/**")
            exclude("**/build/**")

            // JVM target for analysis
            jvmTarget = "21"

            // Report configuration
            reports {
                html.required.set(true)
                xml.required.set(false)
                txt.required.set(false)
                sarif.required.set(false)
                md.required.set(false)
            }
        }
    }
}
