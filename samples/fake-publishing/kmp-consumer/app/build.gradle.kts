// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    id("fakt-sample-kmp")
}

// Published artifact coordinates from kmp-publisher
val publisherGroup = "com.rsicarelli.fakt.samples.publisher"
val publisherVersion = "1.0.0-LOCAL"

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                // Production dependency: API interfaces only
                implementation("$publisherGroup:api:$publisherVersion")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)

                // TEST DEPENDENCY: Published fakes from external artifact
                // This is the key validation - can we use fakes from published JAR?
                implementation("$publisherGroup:api-fakes:$publisherVersion")
            }
        }
    }
}
