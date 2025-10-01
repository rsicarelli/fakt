// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  // Core Kotlin dependencies
  implementation(libs.kotlin.stdlib)

  // Test dependencies
  testImplementation(libs.kotlin.test)
  testImplementation(libs.junit.jupiter)
}

// Configure JVM test task
tasks.named<Test>("test") {
  useJUnitPlatform()

  // Performance tests may need longer timeout
  systemProperty("junit.jupiter.execution.timeout.default", "60s")
}

// JVM target (following Fakts conventions)
kotlin {
  jvmToolchain(21)
}

// Disable API checks for internal performance module
tasks.named("apiCheck") {
  enabled = false
}
