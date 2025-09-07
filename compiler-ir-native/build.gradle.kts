// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    targetCompatibility = JavaVersion.VERSION_21
    sourceCompatibility = JavaVersion.VERSION_21
}

description = "KtFake IR-Native Compiler Module - Next Generation Architecture"

// IR-Native main module aggregates all submodules
dependencies {
    api(project(":compiler-ir-native:ktfake-analysis"))
    api(project(":compiler-ir-native:ktfake-type-system"))
    api(project(":compiler-ir-native:ktfake-codegen-core"))
    api(project(":compiler-ir-native:ktfake-codegen-ir"))
    api(project(":compiler-ir-native:ktfake-diagnostics"))
    api(project(":compiler-ir-native:ktfake-config"))

    // Kotlin compiler API dependencies
    implementation(libs.kotlin.compilerEmbeddable)

    // Testing
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit.jupiter)
}

// TODO: Replace string-based compiler once IR-Native is validated
kotlin {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}
