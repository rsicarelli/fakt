// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Root build file for kmp-multi-module sample
// This demonstrates industry-standard DDD architecture with vertical slices

plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT" apply false
}
