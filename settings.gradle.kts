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
    ":compiler-api",
    ":gradle-plugin",
    ":runtime",
)

// Optional: Include samples for unified IDE workspace view
// Note: Samples are independent composite builds with their own settings.gradle.kts
// Dependency substitution happens in sample settings.gradle.kts via includeBuild("../..")
includeBuild("samples/single-module")
includeBuild("samples/multi-module")

rootProject.name = "fakt"
