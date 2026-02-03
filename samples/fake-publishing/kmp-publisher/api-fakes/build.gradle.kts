// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("fakt-sample-kmp")
    id("fakt-publishing")
    alias(libs.plugins.fakt)
}

description = "Publisher Sample API Fakes - Pre-generated fakes for external consumption"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // CRITICAL: Expose API types transitively so consumers get both
                api(projects.api)

                // Dependencies required by generated fakes
                implementation(libs.coroutines)
            }
        }
    }
}

// Configure Fakt in COLLECTOR MODE
fakt {
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.DEBUG)

    @OptIn(com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule::class)
    collectFakesFrom(projects.api)
}
