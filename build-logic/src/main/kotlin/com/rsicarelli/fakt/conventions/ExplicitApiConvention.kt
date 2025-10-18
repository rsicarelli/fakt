// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension

/**
 * Explicit API mode convention.
 *
 * Configures:
 * - Explicit API mode ONLY for the 'runtime' module
 * - Forces explicit visibility modifiers and return types
 *
 * Rationale:
 * - runtime: Public API consumed by users → requires explicit API
 * - compiler: Internal implementation → no explicit API needed
 * - gradle-plugin: Internal implementation → no explicit API needed
 *
 * Configuration Cache Friendly:
 * - No afterEvaluate usage
 * - Direct configuration using configure extension
 */
fun Project.applyExplicitApiForRuntime() {
    // Enable explicit API mode only for runtime (public API)
    // Compiler and gradle-plugin are internal implementations
    if (name == "runtime") {
        // Works for both org.jetbrains.kotlin.jvm and org.jetbrains.kotlin.multiplatform
        // No need for afterEvaluate with modern Kotlin Gradle Plugin
        configure<KotlinProjectExtension> {
            explicitApi()
        }
    }
}
