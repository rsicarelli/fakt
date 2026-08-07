// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    kotlin("jvm") version "2.4.10"
    // Apply the Fakt plugin straight from the published SNAPSHOT.
    id("com.rsicarelli.fakt") version "1.0.0-beta12-SNAPSHOT"
}

dependencies {
    // The @Fake annotation, at the same SNAPSHOT version.
    implementation("com.rsicarelli.fakt:annotations:1.0.0-beta12-SNAPSHOT")

    // Generated fakes track call history via kotlinx-coroutines StateFlow, so it must be on the
    // classpath that compiles them (the test source set).
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }
