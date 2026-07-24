// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Isolated Mockative competitor (KSP-generated mocks). Mockative is the highest-risk module on a
// bleeding-edge Kotlin: its Gradle plugin pins a KSP version that must match the Kotlin compiler.
// The benchmark runner executes each competitor as a SEPARATE Gradle invocation, so if this module
// fails to configure/compile the other five still produce a table. Adjust the `mockative`/`ksp`
// versions in gradle/libs.versions.toml if CI reports a KSP↔Kotlin mismatch.
plugins {
    id("fakt-benchmark-jvm")
    // KSP must be applied explicitly (with a version) so the io.mockative plugin can hook it and
    // generate the mock API into the test source set.
    alias(libs.plugins.ksp)
    alias(libs.plugins.mockative)
}

dependencies {
    implementation(project(":contracts"))

    testImplementation(project(":harness"))
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockative)
}
