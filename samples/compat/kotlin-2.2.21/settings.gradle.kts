// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

val faktVersion =
    file("../../../gradle.properties").useLines { lines ->
        lines.first { it.startsWith("version=") }.substringAfter("=")
    }

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }

    versionCatalogs {
        create("libs") {
            version("kotlin", "2.2.21")
            version("fakt", faktVersion)
            version("kotlinx-coroutines", "1.10.2")

            plugin("kotlin-multiplatform", "org.jetbrains.kotlin.multiplatform").versionRef("kotlin")
            plugin("fakt", "com.rsicarelli.fakt").versionRef("fakt")

            library("fakt-annotations", "com.rsicarelli.fakt", "annotations").versionRef("fakt")
            library("kotlin-test", "org.jetbrains.kotlin", "kotlin-test").versionRef("kotlin")
            library("coroutines-test", "org.jetbrains.kotlinx", "kotlinx-coroutines-test").versionRef("kotlinx-coroutines")
        }
    }
}

rootProject.name = "fakt-compat-kotlin-2.2.21"
