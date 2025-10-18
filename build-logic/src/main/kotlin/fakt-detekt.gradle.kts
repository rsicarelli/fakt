// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

/**
 * Detekt static analysis convention plugin.
 *
 * Applies Detekt to individual projects (replaces allprojects usage).
 * This plugin should be applied to each module that needs static analysis.
 *
 * Configuration Cache Friendly:
 * - No allprojects usage
 * - Lazy task configuration with withType
 * - Modular application
 */
plugins {
    id("io.gitlab.arturbosch.detekt")
}

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

    // Use baseline file if it exists (for compiler module)
    val baselineFile = file("detekt-baseline.xml")
    if (baselineFile.exists()) {
        baseline.set(baselineFile)
    }

    // Report configuration
    reports {
        html.required.set(true)
        xml.required.set(false)
        txt.required.set(false)
        sarif.required.set(false)
        md.required.set(false)
    }
}
