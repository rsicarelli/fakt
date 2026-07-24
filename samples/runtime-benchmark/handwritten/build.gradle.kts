// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Hand-written fakes baseline — NO mocking dependency at all. If Fakt lands near this while both
// beat the reflection libraries, that is the most credible possible result: Fakt gives you the
// hand-written speed without the hand-written boilerplate.
plugins {
    id("fakt-benchmark-jvm")
}

dependencies {
    implementation(project(":contracts"))

    testImplementation(project(":harness"))
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
}
