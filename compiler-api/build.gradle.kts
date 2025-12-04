// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-kotlin-jvm")
    id("fakt-publishing")
    id("fakt-spotless")
    id("fakt-ktlint")
    id("fakt-detekt")
    alias(libs.plugins.kotlin.serialization)
}

description = "Fakt compiler API for plugin configuration and telemetry"

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}
