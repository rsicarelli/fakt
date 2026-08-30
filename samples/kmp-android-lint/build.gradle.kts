// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.fakt)
}

kotlin {
    // Modern KMP + Android integration (com.android.kotlin.multiplatform.library). Host unit tests
    // are enabled so the `androidHostTest` source set — and its `lintAnalyzeAndroidHostTest` task
    // (the one named in issue #129) — exist. This sample is an end-to-end SMOKE TEST: it proves the
    // fix builds and lints cleanly on Gradle 9.6.1 with an experimental-path FaktGenerateTask. The
    // deterministic regression guard for #129 is the unit test in
    // SimplifiedSourceSetConfigurationTest (a builder-less commonTest srcDir has no build
    // dependency on its producer); reproducing the hard lint failure requires the reporter's
    // multi-module wiring, which this single module does not fully recreate.
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
    // The generate-task path (Fakt's default) registers `faktGenerateMetadataCommonMain`,
    // reproducing the reporter's configuration.
    logLevel.set(com.rsicarelli.fakt.compiler.api.LogLevel.INFO)
}
