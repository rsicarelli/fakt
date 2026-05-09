// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-kotlin-jvm")
    id("fakt-spotless")
    id("fakt-detekt")
}

description =
    "Headless K2 runner — boots Fakt's FIR + IR pipeline outside compileKotlin* for cache-correct codegen (issue #79)"

kotlin {
    compilerOptions { optIn.addAll("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi") }
}

dependencies {
    implementation(projects.compilerApi)
    implementation(projects.codegenRuntime)
    implementation(projects.compiler)

    compileOnly(libs.kotlin.compilerEmbeddable)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.kotlin.compilerEmbeddable)
    testImplementation(libs.kotlin.compileTesting)
    testImplementation(projects.annotations)
}
