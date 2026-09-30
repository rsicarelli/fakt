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
        commonMain.dependencies { implementation("com.rsicarelli.fakt:annotations:$faktVersion") }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
