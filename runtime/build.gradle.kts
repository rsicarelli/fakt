// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-multiplatform")
    id("fakt-spotless")
    id("fakt-ktlint")
    id("fakt-detekt")
    alias(libs.plugins.mavenPublish)
}

kotlin {
    applyDefaultHierarchyTemplate()
    jvm()
    js(IR) {
        browser()
        nodejs()
    }

    // Native targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    macosX64()
    macosArm64()

    linuxX64()
    linuxArm64()
    mingwX64()

    // WASM
    wasmJs {
        nodejs() // Choose nodejs environment to eliminate warning
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.coroutines)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)
                implementation(libs.kotlin.reflect)
            }
        }

        jvmMain {
            dependencies {
                // JVM-specific runtime support
            }
        }

        jvmTest {
            dependencies {
                implementation(libs.junit.jupiter)
            }
        }
    }
}

// Configure JVM test task
tasks.named<Test>("jvmTest") {
    useJUnitPlatform()

    // Standard timeout for runtime tests
    systemProperty("junit.jupiter.execution.timeout.default", "30s")
}
