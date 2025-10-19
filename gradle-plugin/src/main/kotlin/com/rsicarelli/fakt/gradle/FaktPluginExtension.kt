// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

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
         *     debug.set(true)  // Enable debug logs
         * }
         * ```
         *
         * **When to use:**
         * - Troubleshooting why fakes aren't being generated
         * - Understanding which source sets are being used
         * - Debugging multi-module collection issues
         */
        val debug: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

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
