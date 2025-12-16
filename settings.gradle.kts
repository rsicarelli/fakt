// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Enable type-safe project accessors (projects.xxx instead of project(":xxx"))
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
    repositories {
        mavenLocal()
        mavenCentral()
        // Maven Central Snapshots Repository
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
        google()
    }
}

include(
    ":compiler",
    ":compiler-api",
    ":gradle-plugin",
    ":annotations",
)

rootProject.name = "fakt"
