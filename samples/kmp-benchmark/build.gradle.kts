// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    id("fakt-sample-kmp")
    alias(libs.plugins.fakt)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.fakt.annotations)
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
    enabled.set(true)
    logLevel.set(LogLevel.INFO)
}
