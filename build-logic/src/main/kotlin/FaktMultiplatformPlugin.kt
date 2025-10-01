// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Convention plugin for Kotlin Multiplatform modules (runtime).
 *
 * Applies:
 * - kotlin-multiplatform plugin
 * - FaktBasePlugin (common configuration)
 *
 * Note: Publishing should be explicitly applied in module build file.
 */
class FaktMultiplatformPlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
    target.pluginManager.apply("fakt-base")
  }
}