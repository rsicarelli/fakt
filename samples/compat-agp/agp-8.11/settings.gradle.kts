// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

val faktVersion =
    file("../../../gradle.properties").useLines { lines ->
        lines.first { it.startsWith("version=") }.substringAfter("=")
    }

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
            version("kotlin", "2.3.20")
            version("agp", "8.11.1")
            version("coroutines", "1.10.2")
            version("fakt", faktVersion)

            plugin("android-library", "com.android.library").versionRef("agp")
            plugin("kotlin-android", "org.jetbrains.kotlin.android").versionRef("kotlin")
            plugin("fakt", "com.rsicarelli.fakt").versionRef("fakt")

            library("fakt-annotations", "com.rsicarelli.fakt", "annotations").versionRef("fakt")
            library("kotlin-test", "org.jetbrains.kotlin", "kotlin-test").versionRef("kotlin")
            library("coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core")
                .versionRef("coroutines")
        }
    }
}

rootProject.name = "fakt-compat-agp-8.11"
