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
                // CRITICAL: Depend on source module to access original types
                api(projects.core.analytics)

                // Dependencies from generated code
                implementation(libs.coroutines)
            }
        }
    }
}

// Configure Fakt plugin in COLLECTOR MODE
fakt {
    @OptIn(com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.analytics)
}
