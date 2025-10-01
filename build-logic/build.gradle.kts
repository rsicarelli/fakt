// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(libs.kotlin.gradlePlugin)
  implementation(libs.kotlin.gradlePlugin.api)
  // Maven publish plugin - using version from catalog
  compileOnly("com.vanniktech.maven.publish:com.vanniktech.maven.publish.gradle.plugin:0.34.0")
  // Spotless plugin - using version from catalog
  compileOnly("com.diffplug.spotless:spotless-plugin-gradle:7.2.1")
}

gradlePlugin {
  plugins {
    register("fakt-base") {
      id = "fakt-base"
      implementationClass = "FaktBasePlugin"
    }
    register("fakt-publishing") {
      id = "fakt-publishing"
      implementationClass = "FaktPublishingPlugin"
    }
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