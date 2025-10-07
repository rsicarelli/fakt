// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
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
                // Features depends on domain (which depends on foundation)
                // Critical: Transitive dependency validation
                implementation(project(":samples:multi-module:domain"))
                implementation(project(":runtime"))
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

                // Features depends on domain → foundation chain
                // Tests need all fakes modules in the dependency tree
                implementation(project(":samples:multi-module:foundation-fakes"))
                implementation(project(":samples:multi-module:domain-fakes"))
                implementation(project(":samples:multi-module:features-fakes"))
            }
        }
    }
}

// Configure Fakt plugin
fakt {
    enabled.set(true)
    debug.set(true)
}
