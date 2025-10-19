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
                implementation(projects.core.analytics)
                implementation(projects.core.logger)
            }
        }

        commonTest {
            dependencies {
                // Use fakes from core modules for testing
                implementation(projects.core.analyticsFakes)
                implementation(projects.core.loggerFakes)
            }
        }
    }
}

fakt {
    debug.set(true)
}
