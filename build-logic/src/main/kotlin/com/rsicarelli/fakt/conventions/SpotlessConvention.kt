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
 * - Uses version catalog directly (no eager property access)
 * - Versions are hardcoded to match libs.versions.toml
 */
fun Project.applySpotlessPredeclare() {
    // Versions must match gradle/libs.versions.toml
    // Hardcoded to avoid eager property resolution (Configuration Cache friendly)
    val ktfmtVersion = "0.56"
    val gjfVersion = "1.28.0"

    configure<SpotlessExtension> {
        predeclareDeps()
    }

    configure<SpotlessExtensionPredeclare> {
        kotlin {
            ktfmt(ktfmtVersion).googleStyle().configure { it.setRemoveUnusedImports(true) }
        }
        kotlinGradle {
            ktfmt(ktfmtVersion).googleStyle().configure { it.setRemoveUnusedImports(true) }
        }
        java {
            googleJavaFormat(gjfVersion).reorderImports(true).reflowLongStrings(true)
        }
    }
}

