// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-kotlin-jvm")
    id("fakt-publishing")
    id("fakt-spotless")
    id("fakt-detekt")
    alias(libs.plugins.kotlin.serialization)
}

description =
    "Fakt code generation runtime — pure analysis and codegen library, callable without a compiler instance"

dependencies {
    implementation(projects.compilerApi)
    implementation(libs.kotlinx.serialization.json)

    compileOnly(libs.kotlin.compilerEmbeddable)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.coroutines.test)
}
