// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    `kotlin-dsl`
}

dependencies {
    // Kotlin Gradle Plugins
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin.api)

    // Convention Plugins (for module conventions)
    implementation(libs.mavenPublish.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.ktlint.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)

    // Root Plugin Dependencies
    implementation(libs.binaryCompatibilityValidator.gradlePlugin)
    implementation(libs.licenseReport.gradlePlugin)

    // Test Dependencies
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
        register("fakt-sample") {
            id = "fakt-sample"
            implementationClass = "FaktSamplePlugin"
        }
    }
}