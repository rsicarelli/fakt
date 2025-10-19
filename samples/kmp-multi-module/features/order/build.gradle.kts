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
                implementation(projects.core.network)
                implementation(projects.core.logger)
                implementation(projects.core.storage)
                implementation(projects.core.analytics)
            }
        }

        commonTest {
            dependencies {
                // Use fakes from core modules for testing
                implementation(projects.core.networkFakes)
                implementation(projects.core.loggerFakes)
                implementation(projects.core.storageFakes)
                implementation(projects.core.analyticsFakes)
            }
        }
    }
}

fakt {
    debug.set(true)
}
