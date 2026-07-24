// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Isolated MockK competitor. Depends on the shared contracts + the MockK runtime ONLY — it never
// sees Fakt, Mockito, Mokkery or Mockative on its classpath.
plugins {
    id("fakt-benchmark-jvm")
}

dependencies {
    implementation(project(":contracts"))

    testImplementation(project(":harness"))
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.jupiter.params)
    testImplementation(libs.mockk)
}
