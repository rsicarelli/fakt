// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyPublishingConvention
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Publishing plugin for Fakt modules that should be published to Maven Central.
 *
 * This plugin should be explicitly applied only to modules that need to be published.
 *
 * Applies:
 * - Publishing convention (group/version from gradle.properties)
 * - Vanniktech maven publish plugin
 * - Maven Central configuration with RELEASE_MODE support
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     id("fakt-kotlin-jvm") // or fakt-multiplatform
 *     id("fakt-publishing")
 * }
 *
 * description = "Module description for POM"
 * ```
 */
class FaktPublishingPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Apply publishing convention (sets group/version + configures Maven Central)
        target.applyPublishingConvention()
    }
}