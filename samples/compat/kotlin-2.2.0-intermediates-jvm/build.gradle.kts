// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

/*
 * Kotlin 2.2.0 with an intermediate source set on an all-JVM project. No target is non-JVM, so
 * Kotlin gives commonMain and `desktopAndServerMain` no compilation of their own and Fakt adds
 * synthetic producers for both (#160, #162). The worker runs -Xfragments on its own newer Kotlin
 * (2.4.10), so this checks that an older user Kotlin works with it. Same shape as samples/kmp-all-jvm.
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
        // An intermediate shared by desktop and server, but not by cli.
        val desktopAndServerMain by creating { dependsOn(commonMain.get()) }
        val desktopAndServerTest by creating { dependsOn(commonTest.get()) }
        getByName("desktopMain").dependsOn(desktopAndServerMain)
        getByName("serverMain").dependsOn(desktopAndServerMain)
        getByName("desktopTest").dependsOn(desktopAndServerTest)
        getByName("serverTest").dependsOn(desktopAndServerTest)

        commonMain {
            dependencies {
                implementation(libs.fakt.annotations)
            }
            kotlin.srcDir("../shared/src/commonMain/kotlin")
        }
        commonTest {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.coroutines.test)
            }
            kotlin.srcDir("../shared/src/commonTest/kotlin")
        }
    }
}
