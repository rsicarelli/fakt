// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Timing harness shared, byte-identical, by every competitor. Depends on nothing but the Kotlin
// stdlib — it can never bias a measurement because it has no knowledge of any mock/fake technology.
plugins {
    id("fakt-sample-jvm")
}
