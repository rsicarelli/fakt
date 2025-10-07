// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin.api)
    // Plugins used in convention plugins - using versions from catalog
    compileOnly("com.vanniktech.maven.publish:com.vanniktech.maven.publish.gradle.plugin:${libs.versions.mavenPublish.get()}")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:${libs.versions.spotless.get()}")
    implementation(libs.ktlint.gradle.plugin)
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:${libs.versions.detekt.get()}")
    // Root plugin dependencies
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:2.1.0-Beta")
    implementation("org.jetbrains.kotlinx.binary-compatibility-validator:org.jetbrains.kotlinx.binary-compatibility-validator.gradle.plugin:${libs.versions.binaryCompatibilityValidator.get()}")

    // Test dependencies for convention plugin testing
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit5"))
}

gradlePlugin {
    plugins {
        register("fakt-root") {
            id = "fakt-root"
            implementationClass = "FaktRootPlugin"
        }
        register("fakt-kotlin-jvm") {
            id = "fakt-kotlin-jvm"
            implementationClass = "FaktKotlinJvmPlugin"
        }
        register("fakt-multiplatform") {
            id = "fakt-multiplatform"
            implementationClass = "FaktMultiplatformPlugin"
        }
        register("fakt-convention") {
            id = "fakt-convention"
            implementationClass = "FaktConventionPlugin"
        }
    }
}