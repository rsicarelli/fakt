// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.compiler.api.LogLevel

/*
 * KMP sample where EVERY target is a JVM target: `jvm("desktop")`, `jvm("server")` and `jvm("cli")`.
 *
 * Kotlin sees no non-JVM target, so it gives `commonMain` (and the intermediate source set below)
 * no compilation of its own. Fakt adds a synthetic producer for each:
 * - `faktGenerateCommonMain`            → fakes declared in commonMain → `commonTest` (#160)
 * - `faktGenerateDesktopAndServerMain`  → fakes declared in desktopAndServerMain, the intermediate
 *                                         source set desktop and server share → `desktopAndServerTest`
 *                                         (#162)
 * - `faktGenerateDesktopMain`, `faktGenerateServerMain`, `faktGenerateCliMain` → one per platform
 *   main → its own test source set
 *
 * Without the common producer, `commonTest` could not find the commonMain fakes (issue #160).
 * Without the intermediate producer, `SessionStore` was never generated (issue #162).
 *
 * `commonMain` declares `expect fun transport()`: `cliMain` has one actual and
 * `desktopAndServerMain` the other, so the analysis of that intermediate level must keep the
 * expect (commonMain) and the actual (intermediate) apart.
 */
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.fakt)
}

kotlin {
    jvm("desktop")
    jvm("server")
    jvm("cli")

    sourceSets {
        // An intermediate source set shared by desktop and server, but not by cli.
        val desktopAndServerMain by creating { dependsOn(commonMain.get()) }
        val desktopAndServerTest by creating { dependsOn(commonTest.get()) }
        getByName("desktopMain").dependsOn(desktopAndServerMain)
        getByName("serverMain").dependsOn(desktopAndServerMain)
        getByName("desktopTest").dependsOn(desktopAndServerTest)
        getByName("serverTest").dependsOn(desktopAndServerTest)

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
