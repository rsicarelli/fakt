// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

// Fakt with call-history tracking DISABLED (@Fake(callHistory = DISABLED)). Identical to :fakt
// otherwise — this column isolates the runtime cost of Fakt's thread-safe StateFlow call history
// by comparing it against the default :fakt column and the hand-written baseline.
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
