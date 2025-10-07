// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-kotlin-jvm")
    `java-gradle-plugin`
    alias(libs.plugins.mavenPublish)
}

dependencies {
    // Compiler API for SourceSetContext serialization
    implementation(project(":compiler-api"))

    // Kotlin Gradle Plugin APIs (compileOnly like Metro)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin.api)

    // Serialization for SourceSetContext
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Test dependencies
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.gradlePlugin)
    testImplementation(libs.kotlin.gradlePlugin.api)
}

gradlePlugin {
    plugins {
        create("faktPlugin") {
            id = "com.rsicarelli.fakt"
            implementationClass = "com.rsicarelli.fakt.gradle.FaktGradleSubplugin"
            displayName = "Fakt Plugin"
            description =
                "High-performance fake generator for Kotlin test environments using FIR + IR compiler plugin architecture"
            version = "1.0.0-SNAPSHOT"
        }
    }
}

tasks {
    // Configure test task
    test {
        // Standard memory for gradle plugin tests
        jvmArgs("-Xmx1g")

        // Standard timeout
        systemProperty("junit.jupiter.execution.timeout.default", "30s")
    }
}
