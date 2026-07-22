// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    // No Fakt plugin here — the consumer only USES the fakes published in the producer's
    // testFixtures artifact. This is the AGP analogue of discussion #109: an Android module
    // consuming another Android module's test fixtures.
    id("fakt-sample-android")
}

dependencies {
    implementation(projects.producer)

    testImplementation(testFixtures(projects.producer))
    testImplementation(kotlin("test"))
}
