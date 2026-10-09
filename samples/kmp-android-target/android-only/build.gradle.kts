// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.fakt)
}

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.fakt.annotations)
            // Generated fakes track call history via kotlinx-coroutines StateFlow.
            implementation(libs.coroutines)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

android {
    namespace = "com.rsicarelli.fakt.samples.kmpandroidtarget.androidOnly"
    compileSdk = 35

    defaultConfig { minSdk = 24 }
}

fakt {
    // INFO so the worker prints "Fakt: tolerated compiler error" lines, which the cache contract
    // forbids (FAKT_FORBID_TOLERATED).
    logLevel.set(LogLevel.INFO)
}
