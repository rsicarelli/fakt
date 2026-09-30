// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
// #152 Native spike fixture. Not part of the build; see spike/native/README.md.
pluginManagement {
    val kotlinVersion = providers.gradleProperty("spike.kotlin").getOrElse("2.4.10")
    val faktVersion = providers.gradleProperty("spike.fakt").getOrElse("1.0.0-beta13")
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        kotlin("multiplatform") version kotlinVersion
        id("com.rsicarelli.fakt") version faktVersion
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "native-spike-fixture"
