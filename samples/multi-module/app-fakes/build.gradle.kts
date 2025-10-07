// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    // Targets (must match app module)
    jvm()
    // TODO: Re-enable JS when foundation module JS is fixed
    // js(IR) {
    //     browser()
    //     nodejs()
    // }

    sourceSets {
        commonMain {
            dependencies {
                // Depend on app module so we can access original interfaces
                api(project(":app"))

                // CRITICAL: App uses all modules, so app-fakes needs them all
                // Generated app fakes reference types from all modules
                api(project(":features"))
                api(project(":domain"))
                api(project(":foundation"))
                api(project(":core"))
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            }
        }
    }
}

// Configure Fakt plugin in COLLECTOR MODE
fakt {
    enabled.set(true)
    debug.set(true)

    // Collect fakes from app module
    collectFakesFrom(project(":app"))
}
