// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    id("fakt-sample-android")
    alias(libs.plugins.fakt)
}

dependencies {
    implementation(libs.fakt.annotations)

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
