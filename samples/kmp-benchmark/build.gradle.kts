// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    id("fakt-sample")
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
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
    enabled.set(true)
    logLevel.set(LogLevel.DEBUG)
}
