// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.LogLevel
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

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
public open class FaktPluginExtension
@Inject
constructor(objects: ObjectFactory, private val project: Project) {
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
     *
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
     *
     * ```kotlin
     * import com.rsicarelli.fakt.compiler.api.LogLevel
     *
     * fakt {
     *     logLevel.set(LogLevel.INFO) // Concise summary with key metrics (default)
     *     logLevel.set(LogLevel.DEBUG) // Detailed breakdown with FIR + IR details (troubleshooting)
     *     logLevel.set(LogLevel.QUIET) // No output except errors (fastest, minimal noise)
     * }
     * ```
     */
    public val logLevel: Property<LogLevel> =
        objects.property(LogLevel::class.java).convention(LogLevel.INFO)

    /**
     * Controls default call history generation for all fakes.
     *
     * When set to `true` (default), generated fakes include:
     * - Call count properties (e.g., `methodNameCallCount`)
     * - Call history storage for argument capture
     * - Verification DSL (`verify { method.wasCalledWith(...) }`)
     *
     * When set to `false`, generated fakes are lightweight and only include behavior configuration
     * without any call tracking infrastructure.
     *
     * Individual fakes can override this default using the `@Fake` annotation:
     * ```kotlin
     * @Fake(callHistory = CallHistoryMode.ENABLED)   // Always generate
     * @Fake(callHistory = CallHistoryMode.DISABLED)  // Never generate
     * @Fake(callHistory = CallHistoryMode.DEFAULT)   // Follow this setting
     * ```
     *
     * **Default:** `true`
     *
     * **Usage:**
     *
     * ```kotlin
     * fakt {
     *     enableCallHistory.set(false)  // Disable call history for all fakes by default
     * }
     * ```
     *
     * **Common use cases:**
     * - Set to `false` for projects that don't need mock-style verification
     * - Set to `true` (default) for migration from mocking frameworks
     * - Use annotation overrides for fine-grained control per interface
     */
    public val enableCallHistory: Property<Boolean> =
        objects.property(Boolean::class.java).convention(true)

    /**
     * Controls default mutability for all generated fakes.
     *
     * When set to `true`, generated fakes are mutable and include a `configure {}` method that
     * allows reconfiguring behaviors mid-test. Behavior properties become `internal var` instead of
     * `private val`.
     *
     * When set to `false` (default), generated fakes are immutable — behaviors are set at
     * construction time and cannot be changed afterwards.
     *
     * Individual fakes can override this default using the `@Fake` annotation:
     * ```kotlin
     * @Fake(mutability = MutabilityMode.MUTABLE)    // Always generate mutable fake
     * @Fake(mutability = MutabilityMode.IMMUTABLE)  // Always generate immutable fake
     * @Fake(mutability = MutabilityMode.DEFAULT)    // Follow this setting
     * ```
     *
     * **Default:** `false`
     *
     * **Usage:**
     *
     * ```kotlin
     * fakt {
     *     enableMutableFakes.set(true)  // Enable mutable fakes for all fakes by default
     * }
     * ```
     *
     * **Common use cases:**
     * - Set to `true` for projects with many integration tests
     * - Set to `false` (default) for unit tests where immutability is preferred
     * - Use annotation overrides for fine-grained control per interface
     */
    public val enableMutableFakes: Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    /**
     * Controls whether generated fakes are placed in the `testFixtures` source set.
     *
     * When set to `true`, Fakt generates fakes into `build/generated/fakt/testFixtures/kotlin/`
     * instead of the default `build/generated/fakt/test/kotlin/`. This enables other modules to
     * consume generated fakes via Gradle's standard `testFixtures()` dependency mechanism:
     * ```kotlin
     * // Consumer module
     * dependencies {
     *     testImplementation(testFixtures(project(":core")))
     * }
     * ```
     *
     * **Requirements:**
     * - The `java-test-fixtures` Gradle plugin must be applied to the project
     * - JVM-only projects (not supported for KMP)
     *
     * If enabled without the `java-test-fixtures` plugin applied, a warning is emitted and Fakt
     * falls back to the default `test` source set behavior.
     *
     * **Default:** `false`
     *
     * **Usage:**
     *
     * ```kotlin
     * plugins {
     *     `java-test-fixtures`
     * }
     *
     * fakt {
     *     useGradleTestFixtures.set(true)
     * }
     * ```
     *
     * @see <a
     *   href="https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures">Gradle
     *   Test Fixtures</a>
     */
    public val useGradleTestFixtures: Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

    /**
     * Opt in to the cache-correct `FaktGenerateTask` codepath that hosts Fakt's compiler in a
     * Gradle Worker outside `compileKotlin*`. Fixes issue #79 — when Gradle's build cache restores
     * `compileKotlin*`, the generated `.kt` files come back too because they're declared task
     * outputs rather than side-effect writes.
     *
     * **Default:** `false` (the existing in-process compiler-plugin path runs unchanged).
     *
     * **Usage (build script):**
     *
     * ```kotlin
     * fakt {
     *     useExperimentalGenerateTask.set(true)
     * }
     * ```
     *
     * **Usage (Gradle property — flips the default for any project that hasn't set it
     * explicitly):**
     *
     * ```
     * gradle build -Pfakt.useExperimentalGenerateTask=true
     * ```
     *
     * Setting the value in the extension wins over the property if both are present.
     *
     * Marked experimental during the rollout (PRs #97 → #100). The default flips in PR 5; this
     * property becomes the explicit opt-out for one minor before the legacy in-process path is
     * removed.
     */
    public val useExperimentalGenerateTask: Property<Boolean> =
        objects.property(Boolean::class.java).convention(false)

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
     *
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
     * This overload enables usage of Gradle's type-safe project accessors for improved IDE support
     * and compile-time validation. Both string-based and type-safe approaches are fully supported.
     *
     * Internally, this extracts the project path from the dependency and resolves it to the actual
     * Project instance. This avoids using deprecated Gradle APIs.
     *
     * @param projectDependency Type-safe project accessor (e.g., projects.core.analytics)
     *
     * **Usage:**
     *
     * ```kotlin*
     * fakt {
     *     collectFakesFrom(projects.core.analytics)
     * }
     * ```
     *
     * **Enable type-safe accessors in settings.gradle.kts:**
     *
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
