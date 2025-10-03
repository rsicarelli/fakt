// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for Kotlin JVM modules (compiler, gradle-plugin).
 *
 * Applies:
 * - kotlin-jvm plugin
 * - FaktBasePlugin (common configuration)
 * - Ktlint formatting
 *
 * Note: Publishing should be explicitly applied in module build file
 * to avoid conflicts with gradle-plugin mechanism.
 */
class FaktKotlinJvmPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.jvm")
        target.applyCommonConventions()
        target.applyKtlintConvention()
    }
}