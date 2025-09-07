// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

description = "KtFake Code Generation Core - Abstract Generation Engine"

dependencies {
    // Code generation core
    implementation(libs.kotlin.compilerEmbeddable)

    // Project dependencies
    api(project(":compiler-ir-native:ktfake-analysis"))
    api(project(":compiler-ir-native:ktfake-type-system"))
    api(project(":compiler-ir-native:ktfake-config"))

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
}
