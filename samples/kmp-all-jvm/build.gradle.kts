// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

/*
 * KMP sample where EVERY target is a JVM target: `jvm("desktop")` and `jvm("server")`.
 *
 * Kotlin sees no non-JVM target, so it gives `commonMain` no compilation of its own. Fakt adds a
 * synthetic producer for it:
 * - `faktGenerateCommonMain`  → fakes declared in commonMain → `build/generated/fakt/commonTest`
 * - `faktGenerateDesktopMain` → fakes declared in desktopMain → `desktopTest`
 * - `faktGenerateServerMain`  → fakes declared in serverMain  → `serverTest`
 *
 * Without the common producer, `commonTest` could not find the commonMain fakes (issue #160).
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

kotlin {
    jvm("desktop")
    jvm("server")

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
