// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyCommonConventions
import com.rsicarelli.fakt.conventions.applyPublishingConvention
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for Kotlin JVM modules (compiler, gradle-plugin, compiler-api).
 *
 * Applies:
 * - Publishing convention (group/version from gradle.properties)
 * - kotlin-jvm plugin
 * - Common conventions (toolchain, compiler, tests)
 *
 * Note:
 * - maven-publish plugin should be explicitly applied in module build file
 * - Ktlint is applied via FaktRootPlugin to all projects
 */
class FaktKotlinJvmPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // Apply publishing convention first (sets group/version)
        target.applyPublishingConvention()

        // Apply Kotlin JVM plugin
        target.pluginManager.apply("org.jetbrains.kotlin.jvm")

        // Apply common conventions (toolchain, compiler, tests)
        target.applyCommonConventions()
    }
}