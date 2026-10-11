// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

/*
 * Every target is Kotlin/Native, so Kotlin builds commonMain as a KotlinSharedNativeCompilation.
 * Fakt must generate its fakes with the Kotlin/Native driver (not the metadata one), because the
 * commonized platform.posix types are only visible there (#152).
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

kotlin {
    linuxX64()
    linuxArm64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        // Module-wide opt-in for the cinterop-backed types (#172 workaround).
        all { languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi") }

        commonMain {
            dependencies {
                implementation(libs.fakt.annotations)
                implementation(libs.coroutines)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
