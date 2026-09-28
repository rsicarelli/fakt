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
 * Platforms whose lone target in a single-target KMP project can own both the common and the
 * platform fakes from one `FaktGenerateTask` (issue #153). They are drivable AND expose a single
 * main compilation. `androidJvm` is drivable but its variant compilations (`debug`, `release`, …)
 * would each claim the same `commonTest` output, so a single-target Android project stays on the
 * in-process plugin; Native is not drivable yet (issue #152).
 */
internal fun isSingleTargetDrivablePlatform(platformTypeName: String): Boolean =
    when (platformTypeName.lowercase()) {
        "jvm",
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
 * @param singleTargetPlatformTypeName the lone real target's `KotlinPlatformType` name when the KMP
 *   project declares exactly one target (no per-source-set `commonMain` compilation exists, see
 *   `singleTargetPlatformTypeName` in FaktGradleSubplugin.kt); `null` for multi-target and non-KMP
 *   projects.
 * @param compilationName the compilation's name (`main`, `commonMain`, `debug`, …).
 * @param platformTypeName the compilation target's `KotlinPlatformType` name.
 */
internal fun routeCompilation(
    hasKotlinSourceSetModel: Boolean,
    isMultiplatform: Boolean,
    singleTargetPlatformTypeName: String?,
    compilationName: String,
    platformTypeName: String,
): CompilationRoute =
    when {
        !hasKotlinSourceSetModel ->
            legacyRoute(
                "this Android module uses AGP's built-in Kotlin support, and the Android Gradle " +
                    "Plugin API that exposes its sources is not visible to Fakt's classloader; " +
                    "put AGP on the same build classpath as Fakt"
            )
        !isMultiplatform ->
            if (isDrivablePlatform(platformTypeName)) {
                CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.REGISTER_PRODUCER)
            } else {
                legacyRoute("the '$platformTypeName' platform cannot be driven from a Gradle task")
            }
        singleTargetPlatformTypeName != null ->
            routeSingleTarget(singleTargetPlatformTypeName, platformTypeName)
        compilationName == FaktGradleSubplugin.COMMON_MAIN_COMPILATION ->
            CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.REGISTER_PRODUCER)
        platformTypeName.equals("common", ignoreCase = true) ->
            CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.SUPPRESS)
        isDrivablePlatform(platformTypeName) ->
            CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.REGISTER_CONSUMER)
        else -> CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.LEGACY_HYBRID)
    }

/**
 * A single-target KMP project has no per-source-set `commonMain` compilation, only KGP's legacy
 * metadata `main` (platformType `common`, empty compile classpath). When the lone target can own
 * everything, its main compilation becomes the single producer and the metadata compilation is
 * suppressed; otherwise every compilation stays on the in-process plugin.
 */
private fun routeSingleTarget(
    singleTargetPlatformTypeName: String,
    platformTypeName: String,
): CompilationRoute =
    when {
        !isSingleTargetDrivablePlatform(singleTargetPlatformTypeName) ->
            legacyRoute(
                "a single-target multiplatform project on '$singleTargetPlatformTypeName' cannot " +
                    "generate its fakes from a Gradle task yet; declaring a second target moves " +
                    "it onto the cache-correct path"
            )
        platformTypeName.equals("common", ignoreCase = true) ->
            CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.SUPPRESS)
        else -> CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.REGISTER_SINGLE_TARGET)
    }

private fun legacyRoute(reason: String): CompilationRoute =
    CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.LEGACY, reason)
