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
                implementation(projects.foundation)
                implementation(projects.domain)
                implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)

                // Features depends on domain → foundation chain
                // Tests need all fakes modules in the dependency tree
                implementation(projects.foundationFakes)
                implementation(projects.domainFakes)
                implementation(projects.featuresFakes)
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
