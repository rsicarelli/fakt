// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
plugins {
    kotlin("multiplatform") version "2.2.10"
    id("dev.rsicarelli.ktfake") // ← This is all the user needs!
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":runtime"))
            }
        }

        // commonTest configuration is automatic with the plugin!
        // No need to manually configure:
        // - kotlin.srcDir("build/generated/...")
        // - Test dependencies (added automatically)
        // - Compiler plugin options (configured automatically)
    }
}

// Optional configuration (all have sensible defaults)
ktfake {
    enabled = true
    debug = false
    generateCallTracking = false
    generateBuilderPatterns = false
    threadSafetyChecks = true
}

// No manual compiler plugin configuration needed!
// The Gradle plugin handles all of this automatically:
// - Source set configuration
// - Generated code directories
// - Compiler plugin registration
// - Runtime dependencies
// - Output directory mapping
