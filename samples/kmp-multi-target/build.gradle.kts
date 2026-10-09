// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

plugins {
    id("fakt-sample-kmp")
    alias(libs.plugins.fakt)
}

// Besides commonMain and the platform mains, this sample declares a fake in `webMain` (the
// intermediate source set js and wasmJs share). `faktGenerateMetadataWebMain` produces it (#162).
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.fakt.annotations)
            }
        }

        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }

        // Platform-specific test dependencies (if needed)
        jvmTest {
            dependencies {
                implementation(kotlin("test-junit5"))
            }
        }
    }
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
