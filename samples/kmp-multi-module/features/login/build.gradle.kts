import LogLevel
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
                implementation(libs.coroutines)

                implementation(projects.core.auth)
                implementation(projects.core.logger)
                implementation(projects.core.storage)
                implementation(projects.core.analytics)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
                implementation(libs.turbine)

                // Use fakes from core modules for testing
                implementation(projects.core.authFakes)
                implementation(projects.core.loggerFakes)
                implementation(projects.core.storageFakes)
                implementation(projects.core.analyticsFakes)
            }
        }
    }
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
