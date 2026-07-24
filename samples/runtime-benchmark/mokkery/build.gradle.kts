// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Isolated Mokkery competitor. Mokkery is itself a Kotlin compiler plugin (IR); it lives only in
// this module, so applying it here never touches the Fakt or reflection modules. The Mokkery Gradle
// plugin adds its own runtime dependency automatically.
plugins {
    id("fakt-benchmark-jvm")
    alias(libs.plugins.mokkery)
}

dependencies {
    implementation(project(":contracts"))

    testImplementation(project(":harness"))
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
}
