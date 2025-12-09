// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.LicenseReportExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * License Report Convention for Fakt.
 *
 * Configures dependency license auditing using gradle-license-report plugin v3.0.1+
 * to ensure all dependencies use licenses compatible with Apache 2.0.
 *
 * Features:
 * - Generates license report in JSON format
 * - Validates licenses against allowed list (allowed-licenses.json)
 * - Fails build if incompatible licenses are detected
 * - Task: `checkLicense` - validate all dependency licenses (provided by plugin)
 *
 * Allowed licenses:
 * - Apache 2.0
 * - MIT
 * - BSD (2-Clause, 3-Clause)
 * - EPL 1.0/2.0
 * - CC0/Public Domain
 *
 * Prohibited licenses (copyleft):
 * - GPL, LGPL, AGPL (any version)
 */
fun Project.applyLicenseReportConvention() {
    // Apply license report plugin (provides checkLicense task)
    pluginManager.apply("com.github.jk1.dependency-license-report")

    // Configure license report
    extensions.configure<LicenseReportExtension> {
        allowedLicensesFile = rootProject.file("allowed-licenses.json")
        renderers = arrayOf(JsonReportRenderer("licenses.json", false))
        filters = arrayOf(LicenseBundleNormalizer())
    }

    logger.info("Fakt: License report convention applied")
}
