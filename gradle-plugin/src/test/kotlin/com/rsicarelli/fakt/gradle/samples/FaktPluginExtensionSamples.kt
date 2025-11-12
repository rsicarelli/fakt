// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.samples

import com.rsicarelli.fakt.compiler.api.LogLevel
import com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule

/**
 * Sample configurations for FaktPluginExtension.
 *
 * These samples are referenced in KDoc with @sample tags and serve as
 * real, compilable examples of plugin configuration patterns.
 */
@Suppress("unused", "UNUSED_VARIABLE") // Samples are referenced in KDoc
object FaktPluginExtensionSamples {
    /**
     * Basic configuration with default settings.
     *
     * Most users don't need any configuration - Fakt works out of the box.
     */
    fun basicConfiguration() {
        // In build.gradle.kts:
        /*
        plugins {
            kotlin("multiplatform") version "2.2.20"
            id("com.rsicarelli.fakt") version "1.0.0-SNAPSHOT"
        }

        fakt {
            // All settings optional - defaults are production-ready
        }
        */
    }

    /**
     * Enabling the plugin explicitly (enabled by default).
     */
    fun enabledConfiguration() {
        // In build.gradle.kts:
        /*
        fakt {
            enabled.set(true)  // Explicitly enable (default)
        }
        */
    }

    /**
     * Disabling the plugin for specific builds.
     */
    fun disabledConfiguration() {
        // In build.gradle.kts:
        /*
        fakt {
            enabled.set(false)  // Disable fake generation
        }
        */
    }

    /**
     * Type-safe log level configuration with IDE autocomplete.
     */
    fun logLevelConfiguration() {
        // In build.gradle.kts:
        /*
        import com.rsicarelli.fakt.compiler.api.LogLevel

        fakt {
            logLevel.set(LogLevel.INFO)    // Default - concise summary
            // logLevel.set(LogLevel.DEBUG)   // Troubleshooting - detailed breakdown
            // logLevel.set(LogLevel.TRACE)   // Deep debugging - exhaustive details
            // logLevel.set(LogLevel.QUIET)   // CI/CD - minimal output
        }
        */
    }

    /**
     * Development configuration with debug logging.
     */
    fun developmentConfiguration() {
        // In build.gradle.kts:
        /*
        import com.rsicarelli.fakt.compiler.api.LogLevel

        fakt {
            logLevel.set(LogLevel.DEBUG)  // Detailed output for development
        }
        */
    }

    /**
     * CI/CD configuration for minimal output.
     */
    fun cicdConfiguration() {
        // In build.gradle.kts:
        /*
        import com.rsicarelli.fakt.compiler.api.LogLevel

        fakt {
            logLevel.set(LogLevel.QUIET)  // Zero overhead for CI/CD
        }
        */
    }

    /**
     * Multi-module collector mode configuration (experimental).
     *
     * This module collects fakes from another module without generating its own.
     */
    @OptIn(ExperimentalFaktMultiModule::class)
    fun collectorModeConfiguration() {
        // In collector-module/build.gradle.kts:
        /*
        @file:OptIn(ExperimentalFaktMultiModule::class)

        fakt {
            collectFakesFrom(project(":source-module"))
        }
        */
    }

    /**
     * Multi-module collector mode with type-safe project accessors.
     *
     * Uses Gradle's type-safe project accessors for improved IDE support
     * and compile-time validation.
     */
    @OptIn(ExperimentalFaktMultiModule::class)
    fun collectorModeWithTypeSafeAccessor() {
        // First, enable in settings.gradle.kts:
        /*
        enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
        */

        // Then in collector-module/build.gradle.kts:
        /*
        @file:OptIn(ExperimentalFaktMultiModule::class)

        fakt {
            collectFakesFrom(projects.core.analytics)  // ✅ Type-safe with IDE autocomplete!
        }
        */
    }

    /**
     * Multi-module pattern with dedicated fake modules.
     *
     * Typical structure:
     * - foundation/ → Generates fakes from @Fake interfaces
     * - foundation-fakes/ → Collects fakes (collector mode)
     * - domain/ → Uses fakes via testImplementation(":foundation-fakes")
     */
    @OptIn(ExperimentalFaktMultiModule::class)
    fun multiModulePattern() {
        // Project structure:
        /*
        root/
        ├── foundation/
        │   └── build.gradle.kts:
        │       fakt {
        │           logLevel.set(LogLevel.INFO)  // Generator mode (default)
        │       }
        │
        ├── foundation-fakes/
        │   └── build.gradle.kts:
        │       @OptIn(ExperimentalFaktMultiModule::class)
        │       fakt {
        │           // String-based (traditional)
        │           collectFakesFrom(project(":foundation"))
        │
        │           // Type-safe accessor (recommended) ✨
        │           collectFakesFrom(projects.foundation)
        │       }
        │
        └── domain/
            └── build.gradle.kts:
                dependencies {
                    testImplementation(project(":foundation-fakes"))  // Use collected fakes
                }
        */
    }

    /**
     * Conditional configuration based on build variant.
     */
    fun conditionalConfiguration() {
        // In build.gradle.kts:
        /*
        val isCI = System.getenv("CI") == "true"

        fakt {
            enabled.set(!isCI)  // Disable in CI if needed
            logLevel.set(
                if (isCI) LogLevel.QUIET else LogLevel.INFO
            )
        }
        */
    }

    /**
     * Complete configuration showing all available options.
     */
    @OptIn(ExperimentalFaktMultiModule::class)
    fun completeConfiguration() {
        // In build.gradle.kts:
        /*
        @file:OptIn(ExperimentalFaktMultiModule::class)

        import com.rsicarelli.fakt.compiler.api.LogLevel

        fakt {
            // Core settings
            enabled.set(true)
            logLevel.set(LogLevel.INFO)

            // Multi-module (optional, enables collector mode)
            // String-based:
            // collectFakesFrom(project(":source-module"))
            // Type-safe accessor:
            // collectFakesFrom(projects.sourceModule)
        }
        */
    }
}
