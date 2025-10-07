// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    // Targets
    jvm()
    // TODO: Re-enable JS when foundation module JS is fixed
    // js(IR) {
    //     browser()
    //     nodejs()
    // }

    sourceSets {
        commonMain {
            dependencies {
                // Domain depends on foundation - critical cross-module test
                implementation(project(":foundation"))
                implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

                // CRITICAL: Cross-module fake consumption
                // Domain depends on foundation, so tests need both fakes modules
                implementation(project(":foundation-fakes"))
                implementation(project(":domain-fakes"))
            }
        }
    }
}

// Configure Fakt plugin
fakt {
    enabled.set(true)
    debug.set(true)
}
