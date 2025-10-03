// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyApiValidationConvention
import com.rsicarelli.fakt.conventions.applyDokkaConvention
import com.rsicarelli.fakt.conventions.applyKtlintToAllProjects
import com.rsicarelli.fakt.conventions.applySpotlessPredeclare
import com.rsicarelli.fakt.conventions.applySpotlessToAllProjects
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Root convention plugin for Fakt project.
 *
 * Applies and configures:
 * - Binary Compatibility Validator (apiValidation)
 * - Dokka documentation
 * - Spotless formatting (predeclareDeps + allprojects)
 * - Ktlint linting (allprojects)
 *
 * Note: This plugin should ONLY be applied to the root project.
 *
 * All configuration logic is delegated to conventions/ for modularity.
 */
class FaktRootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "FaktRootPlugin can only be applied to the root project"
        }

        with(target) {
            // Apply plugins
            pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")
            pluginManager.apply("org.jetbrains.dokka")
            pluginManager.apply("com.diffplug.spotless")

            // Apply conventions (all logic in conventions/ directory)
            applyApiValidationConvention()
            applyDokkaConvention()
            applySpotlessPredeclare()
            applySpotlessToAllProjects()
            applyKtlintToAllProjects()
        }
    }
}
