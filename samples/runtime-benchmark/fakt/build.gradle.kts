// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

// The ONLY module that applies the Fakt compiler plugin. Its @Fake interfaces live in main; the
// generated fakes land in the test source set, which is exactly where the benchmark runs.
plugins {
    id("fakt-benchmark-jvm")
    alias(libs.plugins.fakt)
}

dependencies {
    implementation(libs.fakt.annotations)

    testImplementation(project(":harness"))
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
}

fakt {
    logLevel.set(LogLevel.QUIET)
}
