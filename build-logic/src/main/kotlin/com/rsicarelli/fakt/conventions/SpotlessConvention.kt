// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessExtensionPredeclare
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Spotless formatting convention.
 *
 * Provides two functions:
 * - applySpotlessPredeclare: Configures formatters (ktfmt, googleJavaFormat)
 * - applySpotlessToAllProjects: Applies formatting to all projects
 */

/**
 * Configure Spotless predeclare with ktfmt and Google Java Format.
 *
 * Configuration Cache Friendly:
 * - Uses version catalog (single source of truth)
 * - No eager property access
 */
fun Project.applySpotlessPredeclare() {
    // Read versions from gradle/libs.versions.toml (single source of truth)
    val ktfmtVersion = libs.version("ktfmt")
    val gjfVersion = libs.version("google-java-format")

    configure<SpotlessExtension> {
        predeclareDeps()
    }

    configure<SpotlessExtensionPredeclare> {
        kotlin {
            ktfmt(ktfmtVersion).kotlinlangStyle().configure { it.setRemoveUnusedImports(true) }
        }
        kotlinGradle {
            ktfmt(ktfmtVersion).kotlinlangStyle().configure { it.setRemoveUnusedImports(true) }
        }
        java {
            googleJavaFormat(gjfVersion).reorderImports(true).reflowLongStrings(true)
        }
    }
}

