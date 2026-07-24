// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.JavaVersion

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.fakt)
}

android {
    namespace = "com.rsicarelli.fakt.compatagp"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // AGP-native test fixtures. Fakt routes generated fakes here (see `useGradleTestFixtures`).
    testFixtures {
        enable = true
    }
}

dependencies {
    implementation(libs.fakt.annotations)

    // Generated fakes track call history with kotlinx-coroutines StateFlow, so both the
    // testFixtures compilation (which compiles the fakes) and the unit test (which uses them via
    // the implicit testFixtures dependency) need coroutines on their classpath.
    testFixturesImplementation(libs.coroutines)
    testImplementation(libs.coroutines)
    testImplementation(libs.kotlin.test)
}

fakt {
    useGradleTestFixtures.set(true)
}
