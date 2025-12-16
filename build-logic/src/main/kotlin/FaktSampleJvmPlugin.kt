// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyJvmCompilation
import com.rsicarelli.fakt.conventions.applyTestConventions
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for Fakt JVM-only sample modules.
 *
 * Applies:
 * - kotlin-jvm plugin
 * - JVM compilation (explicit Java 11 target, no toolchains)
 * - Common test dependencies (JUnit 5, parallel execution)
 */
class FaktSampleJvmPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply Kotlin JVM plugin
            pluginManager.apply("org.jetbrains.kotlin.jvm")

            // Apply JVM compilation (explicit target, no toolchains)
            applyJvmCompilation()

            // Apply test conventions (JUnit Platform, etc.)
            applyTestConventions()
        }
    }
}
