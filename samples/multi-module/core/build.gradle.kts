// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

kotlin {
    jvmToolchain(21)

    // Use KMP default hierarchy template
    applyDefaultHierarchyTemplate()

    // Targets matching api module
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
                api(project(":api"))
                implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")

                // Core depends on api
                // Tests need both api-fakes and core-fakes
                implementation(project(":api-fakes"))
                implementation(project(":core-fakes"))
            }
        }
    }
}

// Configure Fakt plugin
fakt {
    enabled.set(true)
    debug.set(true)
}
