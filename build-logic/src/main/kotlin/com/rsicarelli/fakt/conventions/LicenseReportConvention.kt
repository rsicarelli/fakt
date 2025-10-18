// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.JsonReportRenderer
import com.github.jk1.license.LicenseReportExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.io.File

/**
 * License Report Convention for Fakt.
 *
 * Configures dependency license auditing to ensure all dependencies
 * use licenses compatible with Apache 2.0.
 *
 * Features:
 * - Generates license report in JSON format
 * - Validates licenses against allowed list (allowed-licenses.json)
 * - Fails build if incompatible licenses are detected
 * - Task: `checkLicense` - validate all dependency licenses
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
    // Apply license report plugin
    pluginManager.apply("com.github.jk1.dependency-license-report")

    // Configure license report
    extensions.configure<LicenseReportExtension> {
        allowedLicensesFile = rootProject.file("allowed-licenses.json")
        renderers = arrayOf(JsonReportRenderer("licenses.json", false))
        filters = arrayOf(LicenseBundleNormalizer())
    }

    // Create checkLicense task (only if it doesn't exist)
    if (tasks.findByName("checkLicense") == null) {
        tasks.register("checkLicense") {
        dependsOn("generateLicenseReport")
        group = "verification"
        description = "Validates all dependency licenses against allowed list"

        doLast {
            val allowedFile = rootProject.file("allowed-licenses.json")
            val licenseFile = layout.buildDirectory.file("reports/dependency-license/licenses.json").get().asFile

            if (!allowedFile.exists()) {
                throw GradleException("Allowed licenses file not found: ${allowedFile.absolutePath}")
            }

            if (!licenseFile.exists()) {
                throw GradleException("License report not generated. Run 'generateLicenseReport' first.")
            }

            // Parse allowed licenses
            val allowedLicenses = groovy.json.JsonSlurper().parse(allowedFile) as Map<*, *>
            val allowedNames = (allowedLicenses["allowedLicenses"] as List<*>).map { it.toString() }

            // Parse generated license report
            val report = groovy.json.JsonSlurper().parse(licenseFile) as Map<*, *>
            val dependencies = report["dependencies"] as List<*>

            val violations = mutableListOf<String>()

            dependencies.forEach { dep ->
                val depMap = dep as Map<*, *>
                val moduleName = "${depMap["moduleName"]}:${depMap["moduleVersion"]}"
                val licenses = depMap["moduleLicenses"] as List<*>

                if (licenses.isEmpty()) {
                    violations.add("$moduleName - NO LICENSE INFORMATION")
                } else {
                    licenses.forEach { lic ->
                        val licMap = lic as Map<*, *>
                        val licName = licMap["moduleLicenseName"] ?: licMap["moduleLicenseUrl"] ?: "Unknown"

                        val isAllowed = allowedNames.any { allowed ->
                            licName.toString().contains(allowed, ignoreCase = true)
                        }

                        if (!isAllowed) {
                            violations.add("$moduleName - INCOMPATIBLE LICENSE: $licName")
                        }
                    }
                }
            }

            if (violations.isNotEmpty()) {
                throw GradleException("""
                    |
                    |❌ LICENSE VIOLATIONS DETECTED:
                    |
                    |${violations.joinToString("\n")}
                    |
                    |Please review dependencies with incompatible licenses.
                    |Allowed licenses are defined in: ${allowedFile.absolutePath}
                """.trimMargin())
            }

            logger.lifecycle("✅ All dependency licenses are compatible")
        }
    }
    }

    logger.info("Fakt: License report convention applied")
}
