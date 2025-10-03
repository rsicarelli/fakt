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
    compileOnly("com.diffplug.spotless:spotless-plugin-gradle:${libs.versions.spotless.get()}")
    implementation(libs.ktlint.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("fakt-kotlin-jvm") {
            id = "fakt-kotlin-jvm"
            implementationClass = "FaktKotlinJvmPlugin"
        }
        register("fakt-multiplatform") {
            id = "fakt-multiplatform"
            implementationClass = "FaktMultiplatformPlugin"
        }
    }
}