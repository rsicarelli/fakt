// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
    id("com.rsicarelli.fakt")
}

val faktVersion = providers.gradleProperty("spike.fakt").getOrElse("1.0.0-beta13")

kotlin {
    applyDefaultHierarchyTemplate()
    linuxX64 {
        compilations.getByName("main").cinterops.create("fixture") {
            definitionFile.set(project.file("src/nativeInterop/cinterop/fixture.def"))
            includeDirs(project.file("src/nativeInterop/cinterop"))
        }
    }
    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    sourceSets {
        // -Pspike.localAnnotations=true compiles Fakt's annotation sources into the fixture: the
        // published annotations klibs carry klib ABI 2.4.0, which Kotlin/Native < 2.4 rejects.
        if (providers.gradleProperty("spike.localAnnotations").isPresent) {
            commonMain { kotlin.srcDir("../../../annotations/src/commonMain/kotlin") }
        } else {
            commonMain.dependencies { implementation("com.rsicarelli.fakt:annotations:$faktVersion") }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        // Module-wide opt-in: an interface-level @OptIn is copied into the fake without its
        // import (pre-existing codegen bug, reported separately from this spike).
        all { languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi") }
    }
}
