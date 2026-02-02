@file:Suppress("UnstableApiUsage")

// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // Include build-logic for convention plugins
    includeBuild("../../../build-logic")

    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        // CRITICAL: mavenLocal FIRST to find published artifacts from kmp-publisher
        mavenLocal()
        mavenCentral()
        google()
    }

    versionCatalogs {
        create("libs") {
            from(files("../../../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "kmp-consumer"

// App module that consumes published fakes
include(":app")
