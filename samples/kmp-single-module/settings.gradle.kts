@file:Suppress("UnstableApiUsage")

// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Enable type-safe project accessors (projects.xxx instead of project(":xxx"))
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    // Include build-logic for convention plugins (fakt-sample, etc.)
    includeBuild("../../build-logic")

    repositories {
        mavenLocal()  // ✅ Use published plugin from mavenLocal
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()  // ✅ Use published runtime from mavenLocal
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
