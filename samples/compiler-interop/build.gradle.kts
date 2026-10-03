// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

// Root build file: only pins the plugin versions so every module shares one classloader.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.fakt) apply false
}

// This sample doesn't apply the fakt-sample-kmp convention plugin, so it relocates the Kotlin/JS
// yarn.lock itself, like samples/kmp-no-jvm does. A committed lock under vendor/kotlin-js-store
// makes kotlin*RestoreYarnLock run, so the lock exists before kotlin*StoreYarnLock reads it
// (otherwise Store races NpmInstall and fails on Gradle 9.5+). The vendor-style path also keeps
// GitHub's Dependency Graph from indexing the lock as a manifest (see #144).
plugins.withType(YarnPlugin::class.java) {
    the<YarnRootExtension>().lockFileDirectory = rootDir.resolve("vendor/kotlin-js-store")
}
