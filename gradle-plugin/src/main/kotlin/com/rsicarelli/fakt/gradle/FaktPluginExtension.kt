// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.LogLevel
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
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
 * **Multi-module usage** (collector mode):
 * ```kotlin
 * @file:OptIn(ExperimentalFaktMultiModule::class)
 *
 * fakt {
 *     // String-based
 *     collectFakesFrom(project(":foundation"))
 *
 *     // Type-safe accessor
 *     collectFakesFrom(projects.foundation)
 * }
 * ```
 */
public open class FaktPluginExtension @Inject constructor(
    objects: ObjectFactory,
    private val project: Project,
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
    public val enabled: Property<Boolean> = objects.property(Boolean::class.java).convention(true)

    /**
     * Controls logging verbosity for the compiler plugin.
     *
     * **Default:** `LogLevel.INFO`
     *
     * **Usage:**
     * ```kotlin
     * import com.rsicarelli.fakt.compiler.api.LogLevel
     *
     * fakt {
     *     logLevel.set(LogLevel.INFO) // Concise summary with key metrics (default)
     *     logLevel.set(LogLevel.DEBUG) // Detailed breakdown by compilation phase
     *     logLevel.set(LogLevel.TRACE) // Exhaustive details (deep debugging)
     *     logLevel.set(LogLevel.QUIET) //No output except errors (fastest, minimal noise)
     * }
     * ```
     */
    public val logLevel: Property<LogLevel> =
        objects.property(LogLevel::class.java).convention(LogLevel.INFO)

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
     * @see collectFakesFrom
     * @see ExperimentalFaktMultiModule
     */
    @ExperimentalFaktMultiModule
    public val collectFrom: Property<Project> = objects.property(Project::class.java)

    /**
     * Configures this module to collect fakes from the specified project.
     *
     * Convenience method for setting [collectFrom]. Switches this module to collector mode.
     *
     * @param project The source project that generates fakes (must have @Fake interfaces)
     *
     * **Usage:**
     * ```kotlin*
     * fakt {
     *     collectFakesFrom(project(":foundation"))
     * }
     * ```
     *
     * @see collectFrom
     * @see ExperimentalFaktMultiModule
     */
    @ExperimentalFaktMultiModule
    public fun collectFakesFrom(project: Project) {
        collectFrom.set(project)
    }

    /**
     * Configures this module to collect fakes from the specified project using type-safe accessor.
     *
     * This overload enables usage of Gradle's type-safe project accessors for improved
     * IDE support and compile-time validation. Both string-based and type-safe approaches
     * are fully supported.
     *
     * Internally, this extracts the project path from the dependency and resolves it
     * to the actual Project instance. This avoids using deprecated Gradle APIs.
     *
     * @param projectDependency Type-safe project accessor (e.g., projects.core.analytics)
     *
     * **Usage:**
     * ```kotlin*
     * fakt {
     *     collectFakesFrom(projects.core.analytics)
     * }
     * ```
     *
     * **Enable type-safe accessors in settings.gradle.kts:**
     * ```kotlin
     * enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
     * ```
     *
     * @see collectFrom
     * @see ExperimentalFaktMultiModule
     */
    @ExperimentalFaktMultiModule
    public fun collectFakesFrom(projectDependency: ProjectDependency) {
        collectFrom.set(project.project(projectDependency.path))
    }
}
