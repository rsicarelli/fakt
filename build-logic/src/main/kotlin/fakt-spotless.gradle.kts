// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.diffplug.gradle.spotless.SpotlessExtension

/**
 * Spotless formatting convention plugin.
 *
 * Applies Spotless formatting to individual projects (replaces allprojects usage).
 * This plugin should be applied to each module that needs formatting.
 *
 * Configuration Cache Friendly:
 * - No allprojects usage
 * - No eager property access
 * - Modular and declarative
 */
plugins {
    id("com.diffplug.spotless")
}

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
