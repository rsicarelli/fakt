// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    id("fakt-sample-android-fixtures")
    alias(libs.plugins.fakt)
}

dependencies {
    implementation(libs.fakt.annotations)

    // Generated fakes track call history with kotlinx-coroutines StateFlow, so the testFixtures
    // compilation (compile*TestFixturesKotlin) needs coroutines on its classpath.
    testFixturesImplementation(libs.coroutines)
}

fakt {
    logLevel.set(LogLevel.DEBUG)
    // Routes generated fakes into the Android testFixtures source set (AGP native test fixtures,
    // enabled by `android { testFixtures { enable = true } }` in the convention plugin) instead of
    // the default `test` source set. The fakes are then published in the testFixtures artifact and
    // consumed cross-module via `testImplementation(testFixtures(project(":producer")))`.
    useGradleTestFixtures.set(true)
}
