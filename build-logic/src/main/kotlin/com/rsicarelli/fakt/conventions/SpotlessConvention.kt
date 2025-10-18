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

/**
 * Apply Spotless formatting to all projects in the build.
 *
 * Configures formatting for:
 * - misc files (gradle, md, gitignore)
 * - Java files (googleJavaFormat)
 * - Kotlin files (ktfmt)
 * - Kotlin Gradle files (ktfmt + license header)
 * - License headers for Kotlin source files
 */
fun Project.applySpotlessToAllProjects() {
    allprojects {
        pluginManager.apply("com.diffplug.spotless")
        configure<SpotlessExtension> {
            format("misc") {
                target("*.gradle", "*.md", ".gitignore")
                trimTrailingWhitespace()
                leadingTabsToSpaces(2)
                endWithNewline()
            }
            java {
                target("src/**/*.java")
                trimTrailingWhitespace()
                endWithNewline()
                targetExclude("**/spotless.java")
                targetExclude("**/src/test/data/**")
                targetExclude("**/*Generated.java")
            }
            kotlin {
                target("src/**/*.kt")
                trimTrailingWhitespace()
                endWithNewline()
                targetExclude("**/spotless.kt")
                targetExclude("**/src/test/data/**")
            }
            kotlinGradle {
                target("*.kts")
                trimTrailingWhitespace()
                endWithNewline()
                licenseHeaderFile(
                    rootProject.file("spotless/spotless.kt"),
                    "(import|plugins|buildscript|dependencies|pluginManagement|dependencyResolutionManagement)",
                )
            }
            // Apply license formatting separately for kotlin files
            format("licenseKotlin") {
                licenseHeaderFile(rootProject.file("spotless/spotless.kt"), "(package|@file:)")
                target("src/**/*.kt")
                targetExclude("**/spotless.kt")
                targetExclude("**/src/test/data/**")
            }
        }
    }
}
