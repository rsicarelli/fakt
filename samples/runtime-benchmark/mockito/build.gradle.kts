// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Isolated Mockito competitor (mockito-core + mockito-kotlin). No other mock/fake technology on the
// classpath. Note: the shared domain is interfaces (Fakt's requirement), so Mockito uses cheap JDK
// dynamic proxies here — the documented final-class `mock-maker-inline` penalty is intentionally
// NOT exercised (see README); this keeps the comparison honest.
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
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}
