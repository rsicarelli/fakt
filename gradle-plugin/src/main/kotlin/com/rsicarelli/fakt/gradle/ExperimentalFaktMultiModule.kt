// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

/**
 * Marks declarations related to Fakt's multi-module fake collection feature as experimental.
 *
 * This annotation indicates that the multi-module API is experimental and subject to change
 * in future releases. The multi-module feature allows dedicated fake modules to collect
 * generated fakes from source modules (e.g., foundation-fakes collecting from foundation).
 *
 * ## Why Experimental?
 *
 * Multi-module fake generation is complex and challenging due to:
 * - Cross-module dependency resolution
 * - Platform-specific compilation in KMP projects
 * - Gradle configuration cache compatibility
 * - Circular dependency prevention
 *
 * The API may evolve based on real-world usage patterns and feedback.
 *
 * ## Opt-In Required
 *
 * To use multi-module features in your build.gradle.kts, you must explicitly opt-in.
 *
 * **Option 1: Local opt-in (recommended)**
 * ```kotlin
 * import com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule
 *
 * fakt {
 *     @OptIn(ExperimentalFaktMultiModule::class)
 *     collectFakesFrom(project(":foundation"))
 * }
 * ```
 *
 * **Option 2: File-level opt-in**
 * ```kotlin
 * @file:OptIn(com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule::class)
 *
 * plugins {
 *     kotlin("multiplatform")
 *     id("com.rsicarelli.fakt")
 * }
 *
 * fakt {
 *     collectFakesFrom(project(":foundation"))
 * }
 * ```
 *
 * Note: Some IDE versions may suggest file-level opt-in, but both forms work correctly.
 *
 * ## Multi-Module Pattern
 *
 * The experimental multi-module pattern enables:
 *
 * ```
 * foundation/              # Source module - generates fakes
 *   ├── src/commonMain/    (interfaces with @Fake)
 *   └── build/generated/fakt/  (generated fakes)
 *
 * foundation-fakes/        # Collector module - collects fakes
 *   fakt {
 *     collectFakesFrom(project(":foundation"))
 *   }
 *
 * domain/                  # Consumer module
 *   dependencies {
 *     testImplementation(project(":foundation-fakes"))
 *   }
 * ```
 *
 * ## Stability Timeline
 *
 * This feature will be marked stable once:
 * 1. API design is validated in production projects
 * 2. Cross-platform compilation edge cases are resolved
 * 3. Performance and incremental compilation are optimized
 * 4. Configuration cache compatibility is fully verified
 *
 * @see FaktPluginExtension.collectFakesFrom
 * @see FakeCollectorTask
 */
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message =
        "Multi-module fake collection is experimental and requires explicit opt-in. " +
            "Add '@OptIn(com.rsicarelli.fakt.gradle.ExperimentalFaktMultiModule::class)' " +
            "before the collectFakesFrom call, or '@file:OptIn(...)' at the top of your build.gradle.kts. " +
            "The API may change in future releases. " +
            "See: https://github.com/rsicarelli/fakt/issues",
)
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.CONSTRUCTOR,
)
@MustBeDocumented
public annotation class ExperimentalFaktMultiModule
