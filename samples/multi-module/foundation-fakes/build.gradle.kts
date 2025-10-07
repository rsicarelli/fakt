// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    // Targets (must match foundation module)
    jvm()
    // TODO: Re-enable JS when foundation module JS is fixed
    // js(IR) {
    //     browser()
    //     nodejs()
    // }

    sourceSets {
        commonMain {
            dependencies {
                // CRITICAL: Collector modules MUST depend on source module
                // Generated fakes reference original types (ConfigService, Logger, etc.)
                api(projects.foundation)
                implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
            }
        }
    }
}

// Configure Fakt plugin in COLLECTOR MODE
fakt {
    enabled.set(true)
    debug.set(true)

    // This is the key: collect fakes from foundation module
    collectFakesFrom(project(":foundation"))
}
