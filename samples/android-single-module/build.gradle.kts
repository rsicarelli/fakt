// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    id("fakt-sample-android")
    alias(libs.plugins.fakt)
}

android {
    buildTypes {
        // Extra build types whose names CONTAIN another variant's name ("debugMinified" contains
        // "debug", "preRelease" contains "release"). Each variant's fakes must reach only that
        // variant's unit tests, never a neighbour's (#164b).
        create("debugMinified") { initWith(getByName("debug")) }
        create("preRelease") { initWith(getByName("release")) }
    }
}

dependencies {
    implementation(libs.fakt.annotations)

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
