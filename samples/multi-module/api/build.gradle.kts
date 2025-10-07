// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(21)

    // Use KMP default hierarchy template
    applyDefaultHierarchyTemplate()

    // Targets
    jvm()
    // TODO: Re-enable JS when foundation module JS is fixed
    // js(IR) {
    //     browser()
    //     nodejs()
    // }

    // Native targets
    linuxX64()
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":runtime"))
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")

                // API has no dependencies, only needs its own fakes
                implementation(project(":samples:multi-module:api-fakes"))
            }
        }
    }
}

// Configure Fakt plugin
fakt {
    enabled.set(true)
    debug.set(true)
}
