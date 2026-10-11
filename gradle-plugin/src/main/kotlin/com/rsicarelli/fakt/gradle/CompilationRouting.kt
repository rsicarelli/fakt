// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

/**
 * Platforms the cache-correct worker can drive from a `FaktGenerateTask`. `K2JVMCompiler` (JVM,
 * Android) and `K2JSCompiler` (Kotlin/JS, and Kotlin/Wasm via `-Xwasm`) ship in
 * `kotlin-compiler-embeddable`; `K2Native` does not, so Native targets are not drivable yet
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
 * main compilation. `androidJvm` is deliberately absent: its variant compilations (`debug`,
 * `release`, …) are consumers and [routeSingleTarget] routes them with its own `androidjvm` branch;
 * Native is not drivable yet (issue #152).
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
 * The facts about one Kotlin compilation that [routeCompilation] decides on.
 *
 * @property name the compilation's name (`main`, `commonMain`, `debug`, …).
 * @property platformTypeName the compilation target's `KotlinPlatformType` name.
 * @property sharedSourceSetOwner who owns the default source set of a metadata-target compilation
 *   (see [assignSourceSetOwners]), or `null` when the compilation has no owner to ask.
 */
internal data class RoutedCompilation(
    val name: String,
    val platformTypeName: String,
    val sharedSourceSetOwner: SourceSetOwner? = null,
)

/**
 * Pure routing behind [FaktGradleSubplugin.applyToCompilation]'s cache-correct branch: decides how
 * one Kotlin compilation generates its fakes. `Project`-free so the full table is unit-testable
 * (mirrors [wiresTestCompile]).
 *
 * `commonMain` is checked before the `common` platform-type guard because the metadata target
 * exposes both the per-source-set `commonMain` compilation (the producer) and a legacy `main`
 * compilation plus shared-source-set metadata compilations (all platformType `common`, no IR phase)
 * — those must be suppressed, not turned into a second producer.
 *
 * @param unreadableSourcesReason why the producer cannot read this project's sources, or `null`
 *   when it can (see `unreadableSourcesReason` in FaktGradleSubplugin.kt).
 * @param isMultiplatform whether the Kotlin Multiplatform plugin is applied.
 * @param singleTargetPlatformTypeName the lone real target's `KotlinPlatformType` name when the KMP
 *   project declares exactly one target (no per-source-set `commonMain` compilation exists, see
 *   `singleTargetPlatformTypeName` in FaktGradleSubplugin.kt); `null` for multi-target and non-KMP
 *   projects.
 * @param compilation the compilation being routed.
 */
internal fun routeCompilation(
    unreadableSourcesReason: String?,
    isMultiplatform: Boolean,
    singleTargetPlatformTypeName: String?,
    compilation: RoutedCompilation,
): CompilationRoute {
    val compilationName = compilation.name
    val platformTypeName = compilation.platformTypeName
    return when {
        unreadableSourcesReason != null -> legacyRoute(unreadableSourcesReason)
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
            routeSharedMetadata(compilationName, compilation.sharedSourceSetOwner)
        isDrivablePlatform(platformTypeName) ->
            CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.REGISTER_CONSUMER)
        else -> CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.LEGACY_HYBRID)
    }
}

/**
 * A metadata-target compilation other than `commonMain`. The legacy `main` compilation and every
 * set the metadata producer does not own (native-shared, synthetic, platform) are suppressed. A set
 * owned by [SourceSetOwner.Metadata] (`webMain`) gets its own producer, whose shape the wiring
 * picks from the compilation.
 */
private fun routeSharedMetadata(compilationName: String, owner: SourceSetOwner?): CompilationRoute =
    if (compilationName != LEGACY_METADATA_COMPILATION && owner is SourceSetOwner.Metadata) {
        CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.REGISTER_PRODUCER)
    } else {
        CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.SUPPRESS)
    }

private const val LEGACY_METADATA_COMPILATION = "main"

/**
 * A single-target KMP project has no per-source-set `commonMain` compilation, only KGP's legacy
 * metadata `main` (platformType `common`, empty compile classpath). When the lone target can own
 * everything, its main compilation becomes the single producer and the metadata compilation is
 * suppressed; otherwise every compilation stays on the in-process plugin.
 *
 * A lone `androidTarget()` is the exception: it has one compilation per variant, so none of them is
 * the single producer. The common fakes come from the synthetic `commonMain` producer (see
 * `SyntheticProducerWiring`), every variant is a consumer, and the metadata `main` is suppressed.
 */
private fun routeSingleTarget(
    singleTargetPlatformTypeName: String,
    platformTypeName: String,
): CompilationRoute =
    when {
        singleTargetPlatformTypeName.equals("androidjvm", ignoreCase = true) ->
            if (platformTypeName.equals("common", ignoreCase = true)) {
                CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.SUPPRESS)
            } else {
                CompilationRoute(FaktGradleSubplugin.CacheCorrectDecision.REGISTER_CONSUMER)
            }
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
