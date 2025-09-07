// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.jvm)
  `java-gradle-plugin`
  // alias(libs.plugins.mavenPublish)
}

kotlin {
  jvmToolchain(21)
}

dependencies {
  implementation(project(":compiler-unified"))
  implementation(libs.kotlin.gradlePlugin)
  implementation(libs.kotlin.gradlePlugin.api)

  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.testJunit5)
  testImplementation(libs.junit.jupiter)
}

gradlePlugin {
  plugins {
    create("ktfakePlugin") {
      id = "dev.rsicarelli.ktfake"
      implementationClass = "dev.rsicarelli.ktfake.gradle.KtFakeGradleSubplugin"
      displayName = "KtFake Plugin"
      description = "High-performance fake generator for Kotlin test environments using FIR + IR compiler plugin architecture"
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

  // Configure functional tests
  register<Test>("functionalTest") {
    description = "Run functional tests for the gradle plugin"
    group = "verification"

    testClassesDirs = sourceSets["functionalTest"].output.classesDirs
    classpath = sourceSets["functionalTest"].runtimeClasspath

    // Functional tests may need more time
    systemProperty("junit.jupiter.execution.timeout.default", "60s")

    // More memory for Gradle integration tests
    jvmArgs("-Xmx2g")
  }

  check {
    dependsOn("functionalTest")
  }
}

// Configure functional test source set
sourceSets {
  create("functionalTest") {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += sourceSets.main.get().output
  }
}

configurations["functionalTestImplementation"].extendsFrom(configurations.testImplementation.get())
