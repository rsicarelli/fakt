@file:Suppress("UnstableApiUsage")

// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Enable type-safe project accessors (projects.xxx instead of project(":xxx"))
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // CRITICAL: This enables local plugin development via composite builds
    // Plugin changes auto-rebuild when running this sample
    includeBuild("../..")
    includeBuild("../../build-logic")

    repositories {
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

// Separate includeBuild for dependency substitution (two planes!)
// This substitutes ALL fakt artifacts with source projects
includeBuild("../..") {
    dependencySubstitution {
        substitute(module("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT"))
            .using(project(":runtime"))
        substitute(module("com.rsicarelli.fakt:compiler-api:1.0.0-SNAPSHOT"))
            .using(project(":compiler-api"))
        substitute(module("com.rsicarelli.fakt:compiler:1.0.0-SNAPSHOT"))
            .using(project(":compiler"))
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "kmp-single-module"
