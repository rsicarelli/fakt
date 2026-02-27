// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    id("fakt-sample-jvm")
    `java-test-fixtures`
    alias(libs.plugins.fakt)
}

kotlin {
    explicitApi()
}

dependencies {
    implementation(libs.fakt.annotations)
    implementation(libs.coroutines)

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)

    testFixturesImplementation(libs.coroutines)
}

fakt {
    logLevel.set(LogLevel.DEBUG)
    useGradleTestFixtures.set(true)
}
