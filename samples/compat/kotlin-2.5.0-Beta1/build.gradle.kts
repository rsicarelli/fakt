// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Canary for the worker version policy (#166): Kotlin 2.5.0-Beta1 is one minor newer than the
// Kotlin that Fakt's worker compiles with, so the build must warn and still pass. CI asserts the
// warning. Bump this to the newest pre-release whenever a newer one appears.

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.fakt.annotations)
            }
            kotlin.srcDir("../shared/src/commonMain/kotlin")
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)
            }
            kotlin.srcDir("../shared/src/commonTest/kotlin")
        }
    }
}
