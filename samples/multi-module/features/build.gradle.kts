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
                // Features depends on both foundation and domain
                // Foundation must be direct dependency because features imports foundation types
                implementation(project(":foundation"))
                implementation(project(":domain"))
                implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

                // Features depends on domain → foundation chain
                // Tests need all fakes modules in the dependency tree
                implementation(project(":foundation-fakes"))
                implementation(project(":domain-fakes"))
                implementation(project(":features-fakes"))
            }
        }

        jvmTest {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:5.10.2")
            }
        }
    }
}

// Configure Fakt plugin
fakt {
    enabled.set(true)
    debug.set(true)
}
