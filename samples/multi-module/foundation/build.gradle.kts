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
    // TODO: Re-enable JS after fixing KLIB duplicate unique_name issue
    // js(IR) {
    //     browser()
    //     nodejs()
    // }

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            }
        }
    }

    // Fix for KLIB duplicate unique_name error with mavenLocal
    // Allows runtime dependency to be resolved from both mavenLocal and project
    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xklib-duplicated-unique-name-strategy=allow-first-with-warning")
                }
            }
        }
    }
}

// Configure Fakt plugin
fakt {
    enabled.set(true)
    debug.set(true)
}
