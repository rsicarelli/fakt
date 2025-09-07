// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.jvm)
}

dependencies {
  testImplementation(project(":compiler-unified"))
  testImplementation(project(":runtime"))

  testImplementation(libs.kotlin.compilerTestFramework)
  testImplementation(libs.kotlin.test)
  testImplementation(libs.kotlin.testJunit5)
  testImplementation(libs.junit.jupiter)
  testImplementation(libs.coroutines.test)
}

tasks {
  // Configure test task for box tests
  test {
    // Box tests need even more memory (compilation + execution)
    jvmArgs("-Xmx4g", "-XX:MaxMetaspaceSize=1g")

    // Extended timeout for box tests (compilation + execution)
    systemProperty("junit.jupiter.execution.timeout.default", "120s")

    // Sequential execution for box tests to prevent interference
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")

    // Use single fork for stability
    maxParallelForks = 1
  }

  // Task to generate compiler tests from test data
  register("generateTests") {
    description = "Generates JUnit tests from test data files"
    group = "build"

    inputs.dir("src/test/data")
    outputs.dir("src/test/kotlin/generated")

    doLast {
      println("Test generation will be implemented when we add test data files")
    }
  }

  // Make compileTestKotlin and test depend on generateTests
  compileTestKotlin {
    dependsOn("generateTests")
  }

  test {
    dependsOn("generateTests")
  }
}
