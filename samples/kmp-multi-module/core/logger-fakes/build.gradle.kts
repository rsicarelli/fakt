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
                // CRITICAL: Depend on source module to access original types
                api(projects.core.logger)

                // Dependencies from generated code
                implementation(libs.coroutines)
            }
        }
    }
}

// Configure Fakt plugin in COLLECTOR MODE
fakt {
    logLevel.set(LogLevel.DEBUG)
    @OptIn(com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule::class)
    collectFakesFrom(project(":core:logger"))
}
