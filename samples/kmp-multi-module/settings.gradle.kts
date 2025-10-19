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

rootProject.name = "kmp-multi-module"

// ============================================================================
// CORE INFRASTRUCTURE MODULES
// Technical concerns: auth, storage, network, logging, analytics
// ============================================================================
include(":core:auth")
include(":core:storage")
include(":core:network")
include(":core:logger")
include(":core:analytics")

// ============================================================================
// FEATURE MODULES (Vertical Slices)
// Each feature contains: domain models, use cases, repositories
// ============================================================================
include(":features:login")
include(":features:order")
include(":features:profile")
include(":features:dashboard")
include(":features:notifications")
include(":features:settings")

// ============================================================================
// APP MODULE
// Lightweight coordinator that integrates all features
// ============================================================================
include(":app")

// ============================================================================
// FAKE COLLECTOR MODULES
// Generate fakes for testing (multi-module pattern)
// Each -fakes module collects @Fake interfaces from its source module
// ============================================================================

// Core fakes (infrastructure layer)
include(":core:auth-fakes")
include(":core:logger-fakes")
include(":core:storage-fakes")
include(":core:network-fakes")
include(":core:analytics-fakes")

// Feature fakes (business layer)
include(":features:login-fakes")
include(":features:order-fakes")
include(":features:profile-fakes")
include(":features:dashboard-fakes")
include(":features:notifications-fakes")
include(":features:settings-fakes")

// App fakes (coordination layer)
include(":app-fakes")
