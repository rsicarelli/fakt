// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-root")
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.atomicfu) apply false
}

// Pass version catalog versions to FaktRootPlugin for Spotless configuration
ext["ktfmtVersion"] = libs.versions.ktfmt.get()
ext["gjfVersion"] = libs.versions.gjf.get()
