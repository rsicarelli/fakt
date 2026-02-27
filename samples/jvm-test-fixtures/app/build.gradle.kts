// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("fakt-sample-jvm")
}

dependencies {
    implementation(projects.core)

    testImplementation(testFixtures(projects.core))
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}
