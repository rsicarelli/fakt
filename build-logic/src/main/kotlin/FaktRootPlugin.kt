// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyApiValidationConvention
import com.rsicarelli.fakt.conventions.applyDokkaConvention
import com.rsicarelli.fakt.conventions.applyLicenseReportConvention
import com.rsicarelli.fakt.conventions.applySpotlessPredeclare
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Root convention plugin for Fakt project.
 *
 * Applies and configures:
 * - Binary Compatibility Validator (apiValidation)
 * - Dokka documentation
 * - License Report (dependency license auditing)
 * - Spotless predeclareDeps
 *
 * Note: This plugin should ONLY be applied to the root project.
 * Individual projects apply fakt-spotless, fakt-ktlint, fakt-detekt as needed.
 */
class FaktRootPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "FaktRootPlugin can only be applied to the root project"
        }

        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.binary-compatibility-validator")
            pluginManager.apply("org.jetbrains.dokka")
            pluginManager.apply("com.diffplug.spotless")

            applyApiValidationConvention()
            applyDokkaConvention()
            applyLicenseReportConvention()
            applySpotlessPredeclare()
        }
    }
}
