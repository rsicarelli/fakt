// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

include(
    ":compiler",
    ":gradle-plugin",
    ":runtime",
    ":samples:single-module",
    ":samples:kmp-comprehensive-test",
    ":samples:published-modules-test",
    ":samples:multi-module:api",
    ":samples:multi-module:core",
    ":samples:multi-module:app"
)

rootProject.name = "fakt"
