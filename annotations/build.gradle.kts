// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("fakt-multiplatform")
    id("fakt-publishing")
    id("fakt-spotless")
    id("fakt-detekt")
}

description = "Fakt annotations for type-safe fake generation (zero runtime overhead)"

kotlin {
    applyDefaultHierarchyTemplate()

    // Tier 1
    jvm()

    iosArm64()
    iosX64()
    iosSimulatorArm64()

    // Tier 2
    macosArm64()
    macosX64()
    linuxX64()
    linuxArm64()

    // Tier 3
    watchosArm64()
    watchosArm32()
    watchosX64()
    watchosSimulatorArm64()

    tvosArm64()
    tvosX64()
    tvosSimulatorArm64()

    mingwX64()

    js(IR) {
        nodejs()
        browser()
        binaries.executable()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        nodejs()
        binaries.executable()
    }

    // FaktMultiplatformPlugin disables the Kotlin Gradle Plugin's default kotlin-stdlib dependency
    // so
    // the published artifact never forces a build-version stdlib onto older consumers
    // (retro-compat,
    // see docs/compatibility.md). The annotations sources still need the stdlib on the compile
    // classpath, so add it back as `compileOnly` — it is present at build time but absent from the
    // published POM/module, leaving every consumer to supply its own stdlib at its own Kotlin
    // version.
    sourceSets { commonMain.dependencies { compileOnly("org.jetbrains.kotlin:kotlin-stdlib") } }
}

// Emit metadata at Kotlin 2.0 so every supported consumer (2.2.0 through 2.4.10) can read the
// published artifact. This overrides KotlinCompilerConvention, whose progressive mode and
// `-Xcontext-parameters` flag (Kotlin 2.2+) are incompatible with languageVersion 2.0.
tasks.withType(KotlinCompilationTask::class.java).configureEach {
    compilerOptions {
        apiVersion.set(KotlinVersion.KOTLIN_2_0)
        languageVersion.set(KotlinVersion.KOTLIN_2_0)
        progressiveMode.set(false)
        freeCompilerArgs.set(emptyList<String>())
    }
}
