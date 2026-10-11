// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

val faktVersion =
    file("../../gradle.properties").useLines { lines ->
        lines.first { it.startsWith("version=") }.substringAfter("=")
    }

rootProject.name = "kmp-android-target"

pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            version("kotlin", "2.4.10")
            // The AGP floor Fakt supports; see samples/compat-agp/agp-8.11.
            version("agp", "8.11.1")
            version("coroutines", "1.10.2")
            version("fakt", faktVersion)

            plugin("kotlin-multiplatform", "org.jetbrains.kotlin.multiplatform")
                .versionRef("kotlin")
            plugin("android-library", "com.android.library").versionRef("agp")
            plugin("fakt", "com.rsicarelli.fakt").versionRef("fakt")

            library("fakt-annotations", "com.rsicarelli.fakt", "annotations").versionRef("fakt")
            library("coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core")
                .versionRef("coroutines")
            // An AAR dependency (androidx.core.util.Consumer lives in core, an .aar).
            library("androidx-core", "androidx.core", "core-ktx").version("1.13.1")
        }
    }
}

include(":shared")
include(":android-only")
include(":flavored")
