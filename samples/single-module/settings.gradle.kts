// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

pluginManagement {
    // CRITICAL: This enables local plugin development via composite builds
    // Plugin changes auto-rebuild when running this sample
    includeBuild("../..")

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

// Separate includeBuild for dependency substitution (two planes!)
// This substitutes the runtime artifact with the source project
includeBuild("../..") {
    dependencySubstitution {
        substitute(module("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT"))
            .using(project(":runtime"))
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        google()
    }

    @Suppress("UnstableApiUsage")
    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "single-module-sample"
