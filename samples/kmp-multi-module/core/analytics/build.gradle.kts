// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("fakt-sample")
    id("com.rsicarelli.fakt")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.fakt.runtime)
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }
    }
}

fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.DEBUG)
}
