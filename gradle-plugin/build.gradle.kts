// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
  id("fakt-kotlin-jvm")
  `java-gradle-plugin`
  alias(libs.plugins.mavenPublish)
}

dependencies {
  implementation(project(":compiler"))
  implementation(libs.kotlin.gradlePlugin)
  implementation(libs.kotlin.gradlePlugin.api)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.testJunit5)
  testImplementation(libs.junit.jupiter)
}

gradlePlugin {
  plugins {
    create("faktPlugin") {
      id = "com.rsicarelli.fakt"
      implementationClass = "com.rsicarelli.fakt.gradle.FaktGradleSubplugin"
      displayName = "Fakt Plugin"
      description = "High-performance fake generator for Kotlin test environments using FIR + IR compiler plugin architecture"
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