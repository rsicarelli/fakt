// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.fakt)
}

kotlin {
    // Modern KMP + Android integration (com.android.kotlin.multiplatform.library). Host unit tests
    // are enabled so the `androidHostTest` source set — and its `lintAnalyzeAndroidHostTest` task —
    // exist. That is the exact task from issue #129: it walks the `commonTest` generated source dir,
    // which must declare `faktGenerateMetadataCommonMain` as its builder or Gradle 9.6+ fails the
    // implicit-dependency check.
    androidLibrary {
        namespace = "com.rsicarelli.fakt.kmpandroidlint"
        compileSdk = 35
        minSdk = 24

        withHostTestBuilder {}
    }

    // A second target so a shared `commonMain`/`commonTest` exists (single-target projects have no
    // common source sets, so the bug cannot arise).
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.fakt.annotations)
            // Generated fakes track call history via kotlinx-coroutines StateFlow.
            implementation(libs.coroutines)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

fakt {
    // The experimental generate-task path (also set in gradle.properties) registers
    // `faktGenerateMetadataCommonMain`, reproducing the reporter's configuration.
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.INFO)
}
