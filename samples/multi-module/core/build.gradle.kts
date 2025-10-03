// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform")
}

kotlin {
    jvmToolchain(21)

    // Use KMP default hierarchy template
    applyDefaultHierarchyTemplate()

    // Targets matching api module
    jvm()
    js(IR) {
        browser()
        nodejs()
    }

    // Native targets
    linuxX64()
    macosX64()
    macosArm64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain {
            dependencies {
                api(project(":samples:multi-module:api"))
                implementation(project(":runtime"))
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
            // Add generated sources directory for commonTest
            kotlin.srcDir("build/generated/ktfake/commonTest/kotlin")
        }

        jvmTest {
            // Add generated sources directory for jvmTest
            kotlin.srcDir("build/generated/ktfake/jvmTest/kotlin")
        }

        jsTest {
            // Add generated sources directory for jsTest
            kotlin.srcDir("build/generated/ktfake/jsTest/kotlin")
        }

        nativeTest {
            // Add generated sources directory for nativeTest
            kotlin.srcDir("build/generated/ktfake/nativeTest/kotlin")
        }
    }
}

// Configure compiler plugin for main compilation tasks only
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    if (!name.contains("Test")) {
        compilerOptions {
            val compilerJar = project.rootProject.project(":compiler").tasks.named("shadowJar").get().outputs.files.singleFile
            freeCompilerArgs.addAll(listOf(
                "-Xplugin=${compilerJar.absolutePath}",
                "-P", "plugin:com.rsicarelli.fakt:enabled=true",
                "-P", "plugin:com.rsicarelli.fakt:debug=true",
                "-P", "plugin:com.rsicarelli.fakt:outputDir=${project.layout.buildDirectory.get().asFile.absolutePath}/generated/ktfake"
            ))
        }
    }
}
