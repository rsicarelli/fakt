// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

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
