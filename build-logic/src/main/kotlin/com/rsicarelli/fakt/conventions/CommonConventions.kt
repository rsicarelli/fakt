// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import org.gradle.api.Project

/**
 * Common conventions orchestrator for all Fakt modules.
 *
 * Applies the following conventions in order:
 * 1. JVM compilation (explicit Java 11 target, no toolchains)
 * 2. Kotlin compiler settings (progressive mode, JVM target, flags)
 * 3. Explicit API mode (for annotations module only)
 * 4. Test configuration (JUnit 5, parallel execution, memory settings)
 *
 * This is the main entry point for configuring Kotlin modules.
 * All logic is delegated to specific convention functions for modularity.
 */
fun Project.applyCommonConventions() {
    applyJvmCompilation()
    applyKotlinCompiler()
    applyExplicitApiForRuntime()
    applyTestConventions()
}
