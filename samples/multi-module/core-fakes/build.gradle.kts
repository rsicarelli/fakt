// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    // Targets (must match core module)
    jvm()
    // TODO: Re-enable JS when foundation module JS is fixed
    // js(IR) {
    //     browser()
    //     nodejs()
    // }

    // Native targets (match core module)
    linuxX64()
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                // Depend on core module so we can access original interfaces
                api(project(":samples:multi-module:core"))
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}

// Configure Fakt plugin in COLLECTOR MODE
fakt {
    enabled.set(true)
    debug.set(true)

    // Collect fakes from core module
    collectFakesFrom(project(":samples:multi-module:core"))
}
