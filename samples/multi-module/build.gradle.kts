// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Root build file for multi-module sample (composite build)
// This declares plugins available to all subprojects

plugins {
    kotlin("multiplatform") version "2.2.10" apply false
    id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT" apply false
}
