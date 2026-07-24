// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.JavaVersion

plugins {
    // AGP 9.0 ships built-in Kotlin support: applying `org.jetbrains.kotlin.android` is rejected
    // ("no longer required for Kotlin support since AGP 9.0"). The 8.x cells still apply it.
    alias(libs.plugins.android.library)
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

    // Generated fakes track call history with kotlinx-coroutines StateFlow, so the testFixtures
    // compilation (which compiles the fakes) needs coroutines on its classpath.
    testFixturesImplementation(libs.coroutines)

    // AGP 9.0 uses built-in Kotlin instead of the standalone kotlin-android (KGP) plugin. KGP
    // provides automatic kotlin-test JVM variant resolution (it resolves a bare
    // `org.jetbrains.kotlin:kotlin-test` to a concrete framework variant, JUnit 4 by default);
    // AGP's built-in Kotlin does not include that feature, so a plain kotlin-test dependency has no
    // test-framework variant selected and fails to resolve. The 8.x cells apply KGP and use
    // `testImplementation(libs.kotlin.test)`; this cell uses plain JUnit 4 instead. (Alternatively,
    // `testImplementation("org.jetbrains.kotlin:kotlin-test-junit")` keeps kotlin.test by pinning
    // the variant explicitly.)
    testImplementation("junit:junit:4.13.2")
}

fakt {
    useGradleTestFixtures.set(true)
}
