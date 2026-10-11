// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

// Root build file for kmp-native-only. Both modules apply the plugins themselves.
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.fakt) apply false
}
