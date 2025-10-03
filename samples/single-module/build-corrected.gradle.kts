// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform") version "2.2.10"
    // Plugin will be tested separately - for now just test basic compilation
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    jvm()

    // ✅ NO MANUAL sourceSets BLOCK!
    // Default hierarchy template automatically creates:
    // - commonMain, commonTest
    // - jvmMain, jvmTest
    // - Proper dependency relationships

    sourceSets {
        // Only configure dependencies, not structure
        commonMain {
            dependencies {
                implementation(project(":runtime"))
            }
        }

        commonTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
            // ✅ Generated code directory will be added by Gradle plugin
        }
    }
}

// Configure compiler plugin ONLY for main compilation tasks (not test)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    // Only apply plugin to main source sets, not test source sets
    if (!name.contains("Test")) {
        compilerOptions {
            // Apply our unified compiler plugin manually for testing
            val compilerJar =
                project.rootProject
                    .project(":compiler")
                    .tasks
                    .named("shadowJar")
                    .get()
                    .outputs.files.singleFile
            println("Fakt compiler plugin path: ${compilerJar.absolutePath}")

            freeCompilerArgs.addAll(
                listOf(
                    "-Xplugin=${compilerJar.absolutePath}",
                    "-P",
                    "plugin:dev.rsicarelli.ktfake:enabled=true",
                    "-P",
                    "plugin:dev.rsicarelli.ktfake:debug=true",
                    "-P",
                    "plugin:dev.rsicarelli.ktfake:outputDir=${project.layout.buildDirectory.get().asFile.absolutePath}/generated/ktfake/commonTest",
                ),
            )
        }
    }
}

// Ensure proper compilation order
tasks.named("compileKotlinJvm") {
    doLast {
        println("JVM compilation completed - fakes should be generated")
    }
}

tasks.named("compileTestKotlinJvm") {
    dependsOn("compileKotlinJvm")
}
