// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

/*
 * Exactly one target, linuxX64: Kotlin gives the module no commonMain compilation, so the lone
 * linuxX64Main task owns both the common fakes (into commonTest) and the platform fakes (#152).
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

kotlin {
    linuxX64()

    sourceSets {
        // Module-wide opt-in for the posix types (#172 workaround).
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
