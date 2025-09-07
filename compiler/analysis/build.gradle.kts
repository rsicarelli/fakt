// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

description = "KtFake Analysis Module - Pure Interface Analysis Logic"

dependencies {
    // Pure analysis - no code generation dependencies
    implementation(libs.kotlin.compilerEmbeddable)

    // Project dependencies
    api(project(":compiler-ir-native:ktfake-config"))

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
}
