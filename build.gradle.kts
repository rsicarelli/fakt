// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-root")
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.atomicfu) apply false
}

// Dokka 2.x multi-module aggregation
dependencies {
    dokka(projects.runtime)
    dokka(projects.compiler)
    dokka(projects.gradlePlugin)
}
