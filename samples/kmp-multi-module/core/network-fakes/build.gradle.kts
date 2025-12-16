
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
                api(projects.core.network)

                // Dependencies from source module
                implementation(libs.coroutines)
            }
        }
    }
}

// Configure Fakt plugin in COLLECTOR MODE
fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.DEBUG)
    @OptIn(com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.core.network)
}
