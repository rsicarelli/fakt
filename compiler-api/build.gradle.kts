// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-kotlin-jvm")
    kotlin("plugin.serialization") version libs.versions.kotlin.get()
    alias(libs.plugins.mavenPublish)
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}
