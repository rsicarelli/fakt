// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.LogLevel
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * Configuration extension for the Fakt plugin.
 *
 * Fakt operates in two modes:
 * - **Generator mode** (default): Generates fakes from @Fake annotated interfaces
 * - **Collector mode**: Collects generated fakes from another module (multi-module projects)
 *
 * **Basic usage** (generator mode):
 * ```kotlin
 * fakt {
 *     // All properties have sensible defaults, configuration is optional
 *     debug.set(true)  // Enable to troubleshoot generation issues
 * }
 * ```
 *
 * **Multi-module usage** (collector mode):
 * ```kotlin
 * @file:OptIn(ExperimentalFaktMultiModule::class)
 *
 * fakt {
 *     collectFakesFrom(project(":foundation"))  // Collect fakes from foundation module
 * }
 * ```
 */
open class FaktPluginExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /**
         * Controls whether the Fakt plugin is active.
         *
         * When set to `false`, the plugin is completely disabled:
         * - No fake generation occurs
         * - No fake collection happens
         * - Compilation behaves as if Fakt wasn't applied
         *
         * **Default:** `true`
         *
         * **Usage:**
         * ```kotlin
         * fakt {
         *     enabled.set(false)  // Disable Fakt entirely
         * }
         * ```
         *
         * **Common use cases:**
         * - Temporarily disable fake generation during debugging
         * - Conditional enabling based on build variants
         * - CI/CD pipeline optimization
         */
        val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

        /**
         * Enables detailed logging from the compiler plugin.
         *
         * **DEPRECATED:** Use [logLevel] instead for granular control.
         *
         * When enabled, Fakt logs:
         * - Detected @Fake interfaces
         * - Generated file paths
         * - Source set configurations
         * - Platform detection results (collector mode)
         *
         * **Default:** `false`
         *
         * **Usage:**
         * ```kotlin
         * fakt {
         *     debug.set(true)  // Enable debug logs (deprecated)
         *     // Better: use logLevel instead
         *     logLevel.set("DEBUG")
         * }
         * ```
         *
         * **When to use:**
         * - Troubleshooting why fakes aren't being generated
         * - Understanding which source sets are being used
         * - Debugging multi-module collection issues
         */
        @Deprecated("Use logLevel instead", ReplaceWith("logLevel.set(\"DEBUG\")"))
        val debug: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

        /**
         * Controls logging verbosity for the compiler plugin (Type-Safe!).
         *
         * Available levels:
         * - **QUIET**: No output except errors (fastest, minimal noise)
         * - **INFO**: Concise summary with key metrics (default, production-ready)
         * - **DEBUG**: Detailed breakdown by compilation phase (troubleshooting)
         * - **TRACE**: Exhaustive details including IR nodes (deep debugging)
         *
         * **Default:** `LogLevel.INFO`
         *
         * **Usage (Type-Safe!):**
         * ```kotlin
         * import com.rsicarelli.fakt.compiler.api.LogLevel
         *
         * fakt {
         *     logLevel.set(LogLevel.INFO)    // ✅ Type-safe with IDE autocomplete!
         *     logLevel.set(LogLevel.DEBUG)   // ✅ Compile-time validation
         *     logLevel.set(LogLevel.TRACE)   // ✅ No typos possible
         *     logLevel.set(LogLevel.QUIET)
         * }
         * ```
         *
         * **Output Examples:**
         *
         * **INFO (default):**
         * ```
         * ✅ 10 fakes generated in 1.2s (6 cached)
         * Discovery: 120ms | Analysis: 340ms | Generation: 580ms
         * Cache hit rate: 40% (6/15)
         * ```
         *
         * **DEBUG:**
         * ```
         * [DISCOVERY] 120ms - 15 interfaces, 3 classes
         * [ANALYSIS] 340ms
         *   ├─ PredicateCombiner (18ms) - NoGenerics
         *   ├─ PairMapper<T,U,K,V> (42ms) ⚠️ - ClassLevel
         * [GENERATION] 580ms (avg 58ms/interface)
         * ```
         *
         * **Performance Impact:**
         * - QUIET: Zero overhead (recommended for CI/CD)
         * - INFO: Negligible (<1ms)
         * - DEBUG: Minor (~5-10ms)
         * - TRACE: Moderate (~20-50ms)
         *
         * **When to use each level:**
         * - **QUIET**: CI/CD builds, minimal output needed
         * - **INFO**: Normal development, production builds
         * - **DEBUG**: Troubleshooting generation issues, performance analysis
         * - **TRACE**: Deep debugging, reporting bugs, understanding IR internals
         */
        val logLevel: Property<LogLevel> = objects.property(LogLevel::class.java).convention(LogLevel.INFO)

        /**
         * Source project to collect generated fakes from (collector mode).
         *
         * When set, this module switches to **collector mode**:
         * - Does NOT generate its own fakes
         * - Collects fakes from the specified source project
         * - Places fakes in appropriate platform source sets (KMP support)
         * - Enables the dedicated fake module pattern
         *
         * **Default:** Not set (generator mode)
         *
         * **Usage:**
         * ```kotlin
         * @file:OptIn(ExperimentalFaktMultiModule::class)
         *
         * fakt {
         *     collectFakesFrom(project(":foundation"))
         * }
         * ```
         *
         * **Multi-module pattern:**
         * ```
         * foundation/              → Generates fakes (has @Fake interfaces)
         * foundation-fakes/        → Collects fakes (this property set)
         * domain/                  → Uses fakes (testImplementation(":foundation-fakes"))
         * ```
         *
         * **Why use this:**
         * - Share fakes across multiple test modules
         * - Avoid circular dependencies
         * - Separate fake generation from usage
         *
         * @see collectFakesFrom
         * @see ExperimentalFaktMultiModule
         */
        @ExperimentalFaktMultiModule
        val collectFrom: Property<Project> = objects.property(Project::class.java)

        /**
         * Configures this module to collect fakes from the specified project.
         *
         * Convenience method for setting [collectFrom]. Switches this module to collector mode.
         *
         * @param project The source project that generates fakes (must have @Fake interfaces)
         *
         * **Usage:**
         * ```kotlin
         * @file:OptIn(ExperimentalFaktMultiModule::class)
         *
         * fakt {
         *     collectFakesFrom(project(":foundation"))
         * }
         * ```
         *
         * @see collectFrom
         * @see ExperimentalFaktMultiModule
         */
        @ExperimentalFaktMultiModule
        fun collectFakesFrom(project: Project) {
            collectFrom.set(project)
        }
    }
