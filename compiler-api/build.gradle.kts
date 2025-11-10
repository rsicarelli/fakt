// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-kotlin-jvm")
    id("fakt-spotless")
    id("fakt-ktlint")
    id("fakt-detekt")
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.mavenPublish)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}
