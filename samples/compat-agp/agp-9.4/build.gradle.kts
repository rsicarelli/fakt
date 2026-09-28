// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    // Applied explicitly: gradle.properties opts this cell out of AGP 9's built-in Kotlin, so KGP
    // (and with it Fakt's cache-correct FaktGenerateTask path) drives Kotlin compilation.
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

// AGP 9's legacy (non-built-in) Kotlin setup no longer aligns KGP's JVM target with
// `compileOptions`, so it is pinned to match `targetCompatibility` above.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
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
