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

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":runtime"))
            }
        }

        jvmTest {
            dependencies {
                implementation("org.jetbrains.kotlin:kotlin-test")
            }
            kotlin.srcDir("build/generated/ktfake/test/kotlin")
        }
    }
}

// Dependencies are now configured in sourceSets above

// Configure compiler plugin for all Kotlin compilation tasks
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        // Apply our unified compiler plugin manually for testing
        val compilerJar = project.rootProject.project(":compiler").tasks.named("shadowJar").get().outputs.files.singleFile
        println("KtFake compiler plugin path: ${compilerJar.absolutePath}")
        freeCompilerArgs.addAll(listOf(
            "-Xplugin=${compilerJar.absolutePath}",
            "-P", "plugin:dev.rsicarelli.ktfake:enabled=true",
            "-P", "plugin:dev.rsicarelli.ktfake:debug=true"
        ))
    }
}

// Ensure jvmTest compilation runs after jvm compilation to generate fakes first
tasks.named("compileKotlinJvm") {
    doLast {
        println("JVM compilation completed - fakes should be generated")
    }
}

tasks.named("compileTestKotlinJvm") {
    dependsOn("compileKotlinJvm")
}
