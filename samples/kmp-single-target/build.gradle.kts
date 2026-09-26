// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

/*
 * KMP sample with exactly ONE target (`jvm()`) — the shape of issue #153.
 *
 * Kotlin gives a single-target project no per-source-set `commonMain` compilation, so there is no
 * common producer. Fakt drives the lone `jvmMain` compilation from one task (`faktGenerateJvmMain`)
 * that owns both halves:
 * - `@Fake` declared in commonMain → `build/generated/fakt/commonTest/kotlin` → commonTest
 * - `@Fake` declared in jvmMain    → `build/generated/fakt/jvm/main/kotlin`   → jvmTest
 *
 * The expect/actual pair (`platformName`) locks that commonMain is compiled as the common fragment.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

kotlin {
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.fakt.annotations)
                implementation(libs.coroutines)
            }
        }

        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)
            }
        }
    }
}

fakt {
    logLevel.set(LogLevel.DEBUG)
}
