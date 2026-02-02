// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.diffplug.gradle.spotless.SpotlessExtension
import com.rsicarelli.fakt.conventions.libs
import com.rsicarelli.fakt.conventions.version

/**
 * Spotless formatting convention plugin with ktfmt.
 *
 * Applies Spotless formatting to individual projects using ktfmt Kotlin style (4-space indent).
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

// Read versions from gradle/libs.versions.toml (single source of truth)
val ktfmtVersion = libs.version("ktfmt")
val gjfVersion = libs.version("google-java-format")

configure<SpotlessExtension> {
    format("misc") {
        target("*.gradle", "*.md", ".gitignore")
        trimTrailingWhitespace()
        leadingTabsToSpaces(2)
        endWithNewline()
    }

    java {
        target("src/**/*.java")
        googleJavaFormat(gjfVersion).reorderImports(true).reflowLongStrings(true)
        trimTrailingWhitespace()
        endWithNewline()
        targetExclude("**/spotless.java")
        targetExclude("**/src/test/data/**")
        targetExclude("**/*Generated.java")
    }

    kotlin {
        target("src/**/*.kt")
        ktfmt(ktfmtVersion).kotlinlangStyle().configure { it.setRemoveUnusedImports(true) }
        trimTrailingWhitespace()
        endWithNewline()
        targetExclude("**/spotless.kt")
        targetExclude("**/src/test/data/**")
    }

    kotlinGradle {
        target("*.kts")
        ktfmt(ktfmtVersion).kotlinlangStyle().configure { it.setRemoveUnusedImports(true) }
        trimTrailingWhitespace()
        endWithNewline()
        licenseHeaderFile(
            rootProject.file("spotless/spotless.kt"),
            "(import|plugins|buildscript|dependencies|pluginManagement|dependencyResolutionManagement)",
        )
    }

    format("licenseKotlin") {
        licenseHeaderFile(rootProject.file("spotless/spotless.kt"), "(package|@file:)")
        target("src/**/*.kt")
        targetExclude("**/spotless.kt")
        targetExclude("**/src/test/data/**")
    }
}
