// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform") version "2.2.10"
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
}

repositories {
    mavenLocal()
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    // Apply default hierarchy template for automatic source set hierarchy
    applyDefaultHierarchyTemplate()

    // Tier 1: Common
    jvm()
    js(IR) {
        nodejs()
        browser()
    }

    // Tier 2: Native platforms
    linuxX64()
    macosX64()
    macosArm64()
    mingwX64()

    // Tier 3: Apple platforms
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    // Tier 4: WASM (experimental)
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
        }
    }
}
