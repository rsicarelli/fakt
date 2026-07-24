// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

rootProject.name = "runtime-benchmark"

pluginManagement {
    includeBuild("../../build-logic")
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        google()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }

    versionCatalogs {
        create("libs") {
            from(files("../../gradle/libs.versions.toml"))
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Shared infrastructure — no mock/fake technology depends on another.
include(":harness") // timing harness only (stdlib) — testImplementation'd by every competitor
include(":contracts") // plain domain interfaces + models — consumed by the non-Fakt competitors

// One isolated module per competing technology. Classpaths never overlap.
include(":fakt") // the ONLY module that applies the Fakt compiler plugin
include(":fakt-nohistory") // same as :fakt but @Fake(callHistory = DISABLED) — isolates history cost
include(":handwritten") // hand-written fakes baseline — no mocking dependency at all
include(":mockk")
include(":mockito")
include(":mokkery")
// Mockative is intentionally NOT included: Mockative 3.3.2's KSP generates its mock API into KMP
// source-set flavors (commonMain/jvmMain/...) and does not wire concrete mocks for cross-module
// @Mockable types into a plain kotlin("jvm") module. The module sources are kept for a future
// KMP-based revival; see README. Re-enable by uncommenting once the source-set wiring is solved.
// include(":mockative")
