// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-kotlin-jvm")
    id("fakt-publishing")
    id("fakt-spotless")
    id("fakt-ktlint")
    id("fakt-detekt")
    `java-gradle-plugin`
}

description = "Fakt Gradle plugin for seamless build integration"

dependencies {
    // Compiler API for SourceSetContext serialization
    implementation(projects.compilerApi)

    // Kotlin Gradle Plugin APIs (compileOnly like Metro)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin.api)

    // Serialization for SourceSetContext
    implementation(libs.kotlinx.serialization.json)

    // Test dependencies
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.testJunit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlin.gradlePlugin)
    testImplementation(libs.kotlin.gradlePlugin.api)
}

gradlePlugin {
    website.set("https://github.com/rsicarelli/fakt")
    vcsUrl.set("https://github.com/rsicarelli/fakt.git")

    plugins {
        create("faktPlugin") {
            id = "com.rsicarelli.fakt"
            implementationClass = "com.rsicarelli.fakt.gradle.FaktGradleSubplugin"
            displayName = "Fakt Plugin"
            description =
                "High-performance fake generator for Kotlin test environments using FIR + IR compiler plugin architecture"
            tags.set(listOf("kotlin", "compiler-plugin", "testing", "fake", "mock"))
            // Version inherited from PublishingConvention (gradle.properties:VERSION_NAME)
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
