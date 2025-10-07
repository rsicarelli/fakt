// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    // Targets (must match features module)
    jvm()
    // TODO: Re-enable JS when foundation module JS is fixed
    // js(IR) {
    //     browser()
    //     nodejs()
    // }

    sourceSets {
        commonMain {
            dependencies {
                // Depend on features module so we can access original interfaces
                api(projects.features)

                // CRITICAL: Also need domain and foundation because generated features fakes
                // reference their types (UserRepository, OrderService, Logger, etc.)
                // This is necessary because features uses implementation(), not api()
                api(projects.domain)
                api(projects.foundation)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)
            }
        }
    }
}

// Configure Fakt plugin in COLLECTOR MODE
fakt {
    enabled.set(true)
    debug.set(true)

    // Collect fakes from features module
    collectFakesFrom(project(":features"))
}
