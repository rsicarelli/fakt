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
 * - Explicit API mode for public-facing modules: 'annotations' and 'gradle-plugin'
 * - Forces explicit visibility modifiers and return types
 *
 * Rationale:
 * - annotations: Public API consumed by users → requires explicit API
 * - gradle-plugin: Public API (users configure `fakt { }` DSL) → requires explicit API
 * - compiler: Internal implementation → no explicit API needed
 *
 * Configuration Cache Friendly:
 * - No afterEvaluate usage
 * - Direct configuration using configure extension
 */
fun Project.applyExplicitApiForRuntime() {
    // Enable explicit API mode for public-facing modules
    // Compiler is internal implementation and doesn't need explicit API
    if (name == "annotations" || name == "gradle-plugin") {
        // Works for both org.jetbrains.kotlin.jvm and org.jetbrains.kotlin.multiplatform
        // No need for afterEvaluate with modern Kotlin Gradle Plugin
        configure<KotlinProjectExtension> {
            explicitApi()
        }
    }
}
