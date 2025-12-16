// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("fakt-sample-kmp")
    id("com.rsicarelli.fakt")
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.fakt.annotations)
                implementation(libs.coroutines)

                implementation(projects.core.analytics)
                implementation(projects.core.logger)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
                implementation(libs.turbine)

                // Use fakes from core modules for testing
                implementation(projects.core.analyticsFakes)
                implementation(projects.core.loggerFakes)
            }
        }
    }
}

fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.DEBUG)
}
