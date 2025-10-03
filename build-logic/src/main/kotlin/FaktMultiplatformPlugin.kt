// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyCommonConventions
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for Kotlin Multiplatform modules (runtime).
 *
 * Applies:
 * - kotlin-multiplatform plugin
 * - Common conventions (toolchain, compiler, tests)
 *
 * Note:
 * - Publishing should be explicitly applied in module build file
 * - Ktlint is applied via FaktRootPlugin to all projects
 */
class FaktMultiplatformPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        target.applyCommonConventions()
    }
}