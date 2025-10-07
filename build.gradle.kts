// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    id("fakt-root")
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.atomicfu) apply false
}

// Set group for all subprojects (required for composite build dependency substitution)
allprojects {
    group = "com.rsicarelli.fakt"
    version = "1.0.0-SNAPSHOT"
}

// Pass version catalog versions to FaktRootPlugin for Spotless configuration
ext["ktfmtVersion"] = libs.versions.ktfmt.get()
ext["gjfVersion"] = libs.versions.gjf.get()

// Dokka 2.x multi-module aggregation
dependencies {
    dokka(projects.runtime)
    dokka(projects.compiler)
    dokka(projects.gradlePlugin)
}
