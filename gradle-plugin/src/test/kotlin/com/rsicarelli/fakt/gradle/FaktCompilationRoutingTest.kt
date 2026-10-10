// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.FaktGradleSubplugin.CacheCorrectDecision
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins [routeCompilation], the pure decision behind how each Kotlin compilation generates its
 * fakes, together with the helpers it and the task wiring rely on ([isDrivablePlatform],
 * [isSingleTargetDrivablePlatform], [wasmCompilerTarget]).
 *
 * Issue #151 moved Kotlin/JS and Kotlin/Wasm platform mains off the in-process plugin
 * (`LEGACY_HYBRID`) onto a `K2JSCompiler`-driven consumer `FaktGenerateTask`. Issue #153 moved
 * single-target JVM/JS/Wasm KMP projects onto one task that owns both the common and the platform
 * fakes. Issue #154 moved Android modules on AGP 9's built-in Kotlin onto the producer path.
 * End-to-end behaviour is locked by the `kmp-multi-target`, `kmp-no-jvm` and `kmp-single-target`
 * cache-correctness CI cells.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktCompilationRoutingTest {

    @Test
    fun `GIVEN multi-target KMP WHEN routing jsMain THEN registers a consumer task`() {
        val decision = routeKmp(compilationName = "main", platformTypeName = "js")

        assertEquals(CacheCorrectDecision.REGISTER_CONSUMER, decision)
    }

    @Test
    fun `GIVEN multi-target KMP WHEN routing wasmJsMain THEN registers a consumer task`() {
        val decision = routeKmp(compilationName = "main", platformTypeName = "wasm")

        assertEquals(CacheCorrectDecision.REGISTER_CONSUMER, decision)
    }

    @Test
    fun `GIVEN multi-target KMP WHEN routing jvmMain THEN registers a consumer task`() {
        val decision = routeKmp(compilationName = "main", platformTypeName = "jvm")

        assertEquals(CacheCorrectDecision.REGISTER_CONSUMER, decision)
    }

    @Test
    fun `GIVEN multi-target KMP WHEN routing a Native main THEN stays on the in-process plugin`() {
        val decision = routeKmp(compilationName = "main", platformTypeName = "native")

        assertEquals(
            CacheCorrectDecision.LEGACY_HYBRID,
            decision,
            "Native is not drivable from a task yet (issue #152).",
        )
    }

    @Test
    fun `GIVEN multi-target KMP WHEN routing commonMain THEN registers the common producer`() {
        val decision = routeKmp(compilationName = "commonMain", platformTypeName = "common")

        assertEquals(CacheCorrectDecision.REGISTER_PRODUCER, decision)
    }

    @Test
    fun `GIVEN multi-target KMP WHEN routing a shared metadata compilation with no owner THEN suppresses it`() {
        val decision = routeKmp(compilationName = "nativeMain", platformTypeName = "common")

        assertEquals(CacheCorrectDecision.SUPPRESS, decision)
    }

    @Test
    fun `GIVEN a metadata compilation owned by the metadata producer WHEN routing it THEN registers a producer`() {
        val decision =
            routeKmp(
                compilationName = "webMain",
                platformTypeName = "common",
                owner = SourceSetOwner.Metadata("webMain"),
            )

        assertEquals(CacheCorrectDecision.REGISTER_PRODUCER, decision)
    }

    @Test
    fun `GIVEN a native shared metadata compilation WHEN routing it THEN suppresses it`() {
        val decision =
            routeKmp(
                compilationName = "nativeMain",
                platformTypeName = "common",
                owner = SourceSetOwner.NativeShared("nativeMain"),
            )

        assertEquals(CacheCorrectDecision.SUPPRESS, decision)
    }

    @Test
    fun `GIVEN synthetic or platform owners WHEN routing their metadata compilation THEN suppresses it`() {
        val owners =
            listOf(
                SourceSetOwner.Synthetic("desktopAndServerMain", "desktop", "desktopMain"),
                SourceSetOwner.Platform("jvm"),
            )

        owners.forEach { owner ->
            val decision =
                routeKmp(compilationName = "sharedMain", platformTypeName = "common", owner = owner)
            assertEquals(CacheCorrectDecision.SUPPRESS, decision, "owner: $owner")
        }
    }

    @Test
    fun `GIVEN the legacy metadata main compilation WHEN routing it with a metadata owner THEN suppresses it`() {
        val decision =
            routeKmp(
                compilationName = "main",
                platformTypeName = "common",
                owner = SourceSetOwner.Metadata("commonMain"),
            )

        assertEquals(CacheCorrectDecision.SUPPRESS, decision)
    }

    @Test
    fun `GIVEN single-target JVM KMP WHEN routing the jvm main THEN registers the single-target task`() {
        val route = routeSingleTarget(target = "jvm", compilationPlatform = "jvm")

        assertEquals(CacheCorrectDecision.REGISTER_SINGLE_TARGET, route.decision)
        assertNull(route.notCacheCorrectReason, "A task-driven project is cache-correct.")
    }

    @Test
    fun `GIVEN single-target JS or Wasm KMP WHEN routing the platform main THEN registers the single-target task`() {
        listOf("js", "wasm").forEach { platform ->
            val route = routeSingleTarget(target = platform, compilationPlatform = platform)

            assertEquals(CacheCorrectDecision.REGISTER_SINGLE_TARGET, route.decision, platform)
        }
    }

    @Test
    fun `GIVEN single-target JVM KMP WHEN routing the legacy metadata main THEN suppresses it`() {
        val route = routeSingleTarget(target = "jvm", compilationPlatform = "common")

        assertEquals(
            CacheCorrectDecision.SUPPRESS,
            route.decision,
            "The platform main owns the common fakes; the metadata main (empty classpath) must " +
                "not generate them a second time.",
        )
    }

    @Test
    fun `GIVEN single-target Android KMP WHEN routing an Android variant THEN registers a consumer task`() {
        listOf("debug", "release", "debugUnitTest", "releaseUnitTest").forEach { variant ->
            val route =
                routeCompilation(
                    unreadableSourcesReason = null,
                    isMultiplatform = true,
                    singleTargetPlatformTypeName = "androidJvm",
                    compilation = RoutedCompilation(variant, "androidJvm"),
                )

            assertEquals(CacheCorrectDecision.REGISTER_CONSUMER, route.decision, variant)
            assertNull(route.notCacheCorrectReason, "$variant is task-driven and cache-correct.")
        }
    }

    @Test
    fun `GIVEN single-target Android KMP WHEN routing the legacy metadata main THEN suppresses it`() {
        val route = routeSingleTarget(target = "androidJvm", compilationPlatform = "common")

        assertEquals(
            CacheCorrectDecision.SUPPRESS,
            route.decision,
            "The synthetic commonMain producer owns the common fakes; the metadata main must not.",
        )
        assertNull(route.notCacheCorrectReason, "A suppressed compilation is not a fallback.")
    }

    @Test
    fun `GIVEN multi-target KMP with androidTarget WHEN routing an Android variant THEN registers a consumer task`() {
        val decision = routeKmp(compilationName = "debug", platformTypeName = "androidJvm")

        assertEquals(CacheCorrectDecision.REGISTER_CONSUMER, decision)
    }

    @Test
    fun `GIVEN single-target Native KMP WHEN routing the native main THEN stays on the in-process plugin with a reason`() {
        val route = routeSingleTarget(target = "native", compilationPlatform = "native")

        assertEquals(CacheCorrectDecision.LEGACY, route.decision, "Native is #152.")
        assertNotNull(route.notCacheCorrectReason, "The fallback must never be silent.")
    }

    @Test
    fun `GIVEN KGP platform type names WHEN checking single-target drivability THEN only JVM JS and Wasm qualify and Android is routed by its own branch`() {
        listOf("jvm", "js", "wasm").forEach { platform ->
            assertTrue(isSingleTargetDrivablePlatform(platform), "$platform must qualify")
        }
        listOf("androidJvm", "native", "common").forEach { platform ->
            assertFalse(isSingleTargetDrivablePlatform(platform), "$platform must not qualify")
        }
    }

    @Test
    fun `GIVEN multi-target KMP WHEN routing a drivable main THEN carries no not-cache-correct reason`() {
        val route =
            routeCompilation(
                unreadableSourcesReason = null,
                isMultiplatform = true,
                singleTargetPlatformTypeName = null,
                compilation = RoutedCompilation("main", "wasm"),
            )

        assertNull(route.notCacheCorrectReason, "A task-driven compilation is cache-correct.")
    }

    @Test
    fun `GIVEN non-KMP project WHEN routing a js main THEN registers a producer task`() {
        val decision =
            routeCompilation(
                    unreadableSourcesReason = null,
                    isMultiplatform = false,
                    singleTargetPlatformTypeName = null,
                    compilation = RoutedCompilation("main", "js"),
                )
                .decision

        assertEquals(CacheCorrectDecision.REGISTER_PRODUCER, decision)
    }

    @Test
    fun `GIVEN unreadable sources WHEN routing any compilation THEN stays on the in-process plugin with a reason`() {
        // `unreadableSourcesReason = "sources unreadable"` now only happens on AGP built-in Kotlin
        // when AGP's
        // variant API is not visible to Fakt; with it visible, the module routes like any other
        // Android module (issue #154).
        val route =
            routeCompilation(
                unreadableSourcesReason = "sources unreadable",
                isMultiplatform = false,
                singleTargetPlatformTypeName = null,
                compilation = RoutedCompilation("debug", "androidJvm"),
            )

        assertEquals(CacheCorrectDecision.LEGACY, route.decision)
        assertEquals("sources unreadable", route.notCacheCorrectReason)
    }

    @Test
    fun `GIVEN AGP built-in Kotlin with a readable variant API WHEN routing a variant THEN registers a producer`() {
        val decision =
            routeCompilation(
                    unreadableSourcesReason = null,
                    isMultiplatform = false,
                    singleTargetPlatformTypeName = null,
                    compilation = RoutedCompilation("debug", "androidJvm"),
                )
                .decision

        assertEquals(CacheCorrectDecision.REGISTER_PRODUCER, decision)
    }

    @Test
    fun `GIVEN KGP platform type names WHEN checking drivability THEN JVM Android JS and Wasm are drivable`() {
        listOf("jvm", "androidJvm", "js", "wasm").forEach { platform ->
            assertTrue(isDrivablePlatform(platform), "$platform must be drivable")
        }
        listOf("native", "common").forEach { platform ->
            assertFalse(isDrivablePlatform(platform), "$platform must not be drivable")
        }
    }

    @Test
    fun `GIVEN KGP wasm target types WHEN mapping to the compiler flag THEN matches -Xwasm-target values`() {
        assertEquals("wasm-js", wasmCompilerTarget(KotlinWasmTargetType.JS))
        assertEquals("wasm-wasi", wasmCompilerTarget(KotlinWasmTargetType.WASI))
        assertNull(wasmCompilerTarget(null), "A Kotlin/JS target must leave the driver in JS mode.")
    }

    private fun routeKmp(
        compilationName: String,
        platformTypeName: String,
        owner: SourceSetOwner? = null,
    ): CacheCorrectDecision =
        routeCompilation(
                unreadableSourcesReason = null,
                isMultiplatform = true,
                singleTargetPlatformTypeName = null,
                compilation = RoutedCompilation(compilationName, platformTypeName, owner),
            )
            .decision

    private fun routeSingleTarget(target: String, compilationPlatform: String): CompilationRoute =
        routeCompilation(
            unreadableSourcesReason = null,
            isMultiplatform = true,
            singleTargetPlatformTypeName = target,
            compilation = RoutedCompilation("main", compilationPlatform),
        )
}
