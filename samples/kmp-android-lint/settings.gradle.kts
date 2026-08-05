// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

val faktVersion =
    file("../../gradle.properties").useLines { lines ->
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
            version("agp", "9.0.0")
            version("kotlin", "2.4.10")
            version("coroutines", "1.10.2")
            version("fakt", faktVersion)

            plugin("kotlin-multiplatform", "org.jetbrains.kotlin.multiplatform")
                .versionRef("kotlin")
            plugin("android-kotlin-multiplatform-library", "com.android.kotlin.multiplatform.library")
                .versionRef("agp")
            plugin("fakt", "com.rsicarelli.fakt").versionRef("fakt")

            library("fakt-annotations", "com.rsicarelli.fakt", "annotations").versionRef("fakt")
            library("coroutines", "org.jetbrains.kotlinx", "kotlinx-coroutines-core")
                .versionRef("coroutines")
        }
    }
}

rootProject.name = "fakt-kmp-android-lint"
