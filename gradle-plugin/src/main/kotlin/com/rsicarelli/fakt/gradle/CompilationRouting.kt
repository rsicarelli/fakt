// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

/**
 * Platforms the cache-correct worker can drive from a `FaktGenerateTask`. `K2JVMCompiler` (JVM,
 * Android) and `K2JSCompiler` (Kotlin/JS, and Kotlin/Wasm via `-Xwasm`) ship in
 * `kotlin-compiler-embeddable`; `K2NativeCompiler` does not, so Native targets are not drivable yet
 * (issue #152).
 */
internal fun isDrivablePlatform(platformTypeName: String): Boolean =
    when (platformTypeName.lowercase()) {
        "jvm",
        "androidjvm",
        "js",
        "wasm" -> true
        else -> false
    }

/**
 * How one Kotlin compilation generates its fakes: the [decision], plus — whenever the decision is
 * [FaktGradleSubplugin.CacheCorrectDecision.LEGACY] — the user-facing reason the module is not
 * cache-correct ([notCacheCorrectReason], surfaced once per module by [warnNotCacheCorrect]).
 */
internal data class CompilationRoute(
    val decision: FaktGradleSubplugin.CacheCorrectDecision,
    val notCacheCorrectReason: String? = null,
)

/**
 * Pure routing behind [FaktGradleSubplugin.applyToCompilation]'s cache-correct branch: decides how
 * one Kotlin compilation generates its fakes. `Project`-free so the full table is unit-testable
 * (mirrors [shouldWireGeneratedDir]).
 *
 * `commonMain` is checked before the `common` platform-type guard because the metadata target
 * exposes both the per-source-set `commonMain` compilation (the producer) and a legacy `main`
 * compilation plus shared-source-set metadata compilations (all platformType `common`, no IR phase)
 * — those must be suppressed, not turned into a second producer.
 *
 * @param hasKotlinSourceSetModel see `hasKotlinSourceSetModel` (FaktGradleSubplugin.kt).
 * @param isMultiplatform whether the Kotlin Multiplatform plugin is applied.
 * @param hasCommonMainProducer see `hasCommonMainProducer` (FaktGradleSubplugin.kt); ignored for
 *   non-KMP projects.
 * @param compilationName the compilation's name (`main`, `commonMain`, `debug`, …).
 * @param platformTypeName the compilation target's `KotlinPlatformType` name.
 */
internal fun routeCompilation(
    hasKotlinSourceSetModel: Boolean,
    isMultiplatform: Boolean,
    hasCommonMainProducer: Boolean,
    compilationName: String,
    platformTypeName: String,
): CompilationRoute =
    when {
        !hasKotlinSourceSetModel ->
            legacyRoute(
                "this Android module uses AGP's built-in Kotlin support, which keeps sources " +
                    "in the variant model rather than Kotlin source sets"
            )
        !isMultiplatform ->
            if (isDrivablePlatform(platformTypeName)) {
                CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.REGISTER_PRODUCER)
            } else {
                legacyRoute("the '$platformTypeName' platform cannot be driven from a Gradle task")
            }
        !hasCommonMainProducer ->
            legacyRoute(
                "a single-target multiplatform project has no commonMain compilation to " +
                    "generate from; declaring a second target moves it onto the cache-correct path"
            )
        compilationName == FaktGradleSubplugin.COMMON_MAIN_COMPILATION ->
            CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.REGISTER_PRODUCER)
        platformTypeName.equals("common", ignoreCase = true) ->
            CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.SUPPRESS)
        isDrivablePlatform(platformTypeName) ->
            CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.REGISTER_CONSUMER)
        else -> CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.LEGACY_HYBRID)
    }

private fun legacyRoute(reason: String): CompilationRoute =
    CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.LEGACY, reason)
