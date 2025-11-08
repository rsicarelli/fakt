// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

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

    // Tier 1
    jvm()

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    // Tier 2
    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()

    // Tier 3
    watchosArm64()
    watchosArm32()
    watchosX64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    mingwX64()

    js(IR) {
        nodejs()
        browser()
        binaries.executable()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {

        }
    }
}
