// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

/**
 * DSL extension for configuring Fakt plugin.
 *
 * Provides the `fakt { }` configuration block in build.gradle files:
 *
 * ```kotlin
 * fakt {
 *     enabled = true
 *     debug = false
 *     reportsDestination = layout.buildDirectory.dir("fakt/reports")
 *
 *     generateCallTracking = true
 *     generateBuilderPatterns = true
 *     threadSafetyChecks = true
 * }
 * ```
 */
open class FaktPluginExtension
    @Inject
    constructor(
        objects: ObjectFactory,
    ) {
        /**
         * Enable or disable the Fakt plugin.
         * When false, no fake generation occurs.
         *
         * Default: true
         */
        val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

        /**
         * Enable debug logging from the compiler plugin.
         * Useful for troubleshooting fake generation issues.
         *
         * Default: false
         */
        val debug: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

        /**
         * Source project to collect fakes from (collector mode).
         * When set, this module will collect generated fakes from the specified project
         * instead of generating its own fakes.
         *
         * This enables the dedicated fake module pattern:
         * - foundation (generates fakes)
         * - foundation-fakes (collects fakes from foundation)
         * - domain (uses foundation-fakes via standard testImplementation)
         *
         * Example:
         * ```kotlin
         * fakt {
         *     collectFakesFrom(project(":foundation"))
         * }
         * ```
         */
        val collectFrom: Property<Project> = objects.property(Project::class.java)

        /**
         * Configure this module to collect fakes from another project.
         * This enables the dedicated fake module pattern (e.g., foundation-fakes).
         *
         * @param project The source project that generates fakes
         */
        fun collectFakesFrom(project: Project) {
            collectFrom.set(project)
        }

        /**
         * Destination directory for compilation reports and generated metadata.
         *
         * Default: build/fakt/reports
         */
        val reportsDestination: DirectoryProperty = objects.directoryProperty()

        /**
         * Generate call tracking functionality for @Fake(trackCalls = true).
         * When false, call tracking annotations are ignored.
         *
         * Default: true
         */
        val generateCallTracking: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

        /**
         * Generate builder pattern support for @Fake(builder = true) on data classes.
         * When false, builder pattern annotations are ignored.
         *
         * Default: true
         */
        val generateBuilderPatterns: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

        /**
         * Enable thread-safety checks and warnings.
         * Validates that fake usage follows thread-safe patterns.
         *
         * Default: true
         */
        val threadSafetyChecks: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

        /**
         * Configure options for performance optimization.
         *
         * @param configure Configuration lambda for performance options
         */
        fun performance(configure: PerformanceOptions.() -> Unit) {
            val options = PerformanceOptions()
            configure(options)
            // Performance options would be applied here
        }

        /**
         * Configure options for code generation.
         *
         * @param configure Configuration lambda for generation options
         */
        fun generation(configure: GenerationOptions.() -> Unit) {
            val options = GenerationOptions()
            configure(options)
            // Generation options would be applied here
        }

        /**
         * Nested configuration class for performance-related options.
         */
        class PerformanceOptions {
            /**
             * Enable incremental compilation for fake generation.
             * Only regenerates fakes when source interfaces change.
             *
             * Default: true
             */
            var incrementalCompilation: Boolean = true

            /**
             * Enable parallel fake generation for large projects.
             *
             * Default: true
             */
            var parallelGeneration: Boolean = true

            /**
             * Cache generated code to speed up subsequent builds.
             *
             * Default: true
             */
            var enableCaching: Boolean = true
        }

        /**
         * Nested configuration class for code generation options.
         */
        class GenerationOptions {
            /**
             * Generate additional utility methods (reset, clearState, etc.).
             *
             * Default: true
             */
            var generateUtilities: Boolean = true

            /**
             * Generate comprehensive documentation for generated code.
             *
             * Default: false (for build speed)
             */
            var generateDocs: Boolean = false

            /**
             * Generate null checks and validation in generated methods.
             *
             * Default: true
             */
            var generateNullChecks: Boolean = true
        }
    }
