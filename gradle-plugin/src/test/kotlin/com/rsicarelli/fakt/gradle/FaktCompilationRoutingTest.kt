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
 * [wasmCompilerTarget], [consumerTaskNameFor]).
 *
 * Issue #151 (gap A of #150) moved Kotlin/JS and Kotlin/Wasm platform mains off the in-process
 * plugin (`LEGACY_HYBRID`) onto a `K2JSCompiler`-driven consumer `FaktGenerateTask`; only Native
 * platform mains still ride the in-process plugin. End-to-end behaviour is locked by the
 * `kmp-multi-target` / `kmp-no-jvm` cache-correctness CI cells.
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
    fun `GIVEN multi-target KMP WHEN routing a shared metadata compilation THEN suppresses it`() {
        val decision = routeKmp(compilationName = "nativeMain", platformTypeName = "common")

        assertEquals(CacheCorrectDecision.SUPPRESS, decision)
    }

    @Test
    fun `GIVEN single-target KMP WHEN routing its js main THEN stays on the in-process plugin with a reason`() {
        val route =
            routeCompilation(
                hasKotlinSourceSetModel = true,
                isMultiplatform = true,
                hasCommonMainProducer = false,
                compilationName = "main",
                platformTypeName = "js",
            )

        assertEquals(CacheCorrectDecision.LEGACY, route.decision, "Single-target KMP is #153.")
        assertNotNull(route.notCacheCorrectReason, "The fallback must never be silent.")
    }

    @Test
    fun `GIVEN multi-target KMP WHEN routing a drivable main THEN carries no not-cache-correct reason`() {
        val route =
            routeCompilation(
                hasKotlinSourceSetModel = true,
                isMultiplatform = true,
                hasCommonMainProducer = true,
                compilationName = "main",
                platformTypeName = "wasm",
            )

        assertNull(route.notCacheCorrectReason, "A task-driven compilation is cache-correct.")
    }

    @Test
    fun `GIVEN non-KMP project WHEN routing a js main THEN registers a producer task`() {
        val decision =
            routeCompilation(
                    hasKotlinSourceSetModel = true,
                    isMultiplatform = false,
                    hasCommonMainProducer = false,
                    compilationName = "main",
                    platformTypeName = "js",
                )
                .decision

        assertEquals(CacheCorrectDecision.REGISTER_PRODUCER, decision)
    }

    @Test
    fun `GIVEN AGP built-in Kotlin WHEN routing any compilation THEN stays on the in-process plugin`() {
        val decision =
            routeCompilation(
                    hasKotlinSourceSetModel = false,
                    isMultiplatform = false,
                    hasCommonMainProducer = false,
                    compilationName = "debug",
                    platformTypeName = "androidJvm",
                )
                .decision

        assertEquals(
            CacheCorrectDecision.LEGACY,
            decision,
            "AGP 9 built-in Kotlin is gap D (#154).",
        )
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

    @Test
    fun `GIVEN platform test source sets WHEN resolving the owning consumer task THEN follows faktGenerate target Main naming`() {
        assertEquals("faktGenerateJsMain", consumerTaskNameFor("jsTest"))
        assertEquals("faktGenerateWasmJsMain", consumerTaskNameFor("wasmJsTest"))
        assertEquals("faktGenerateJvmMain", consumerTaskNameFor("jvmTest"))
    }

    @Test
    fun `GIVEN commonTest or a non-test source set WHEN resolving the owning consumer task THEN returns null`() {
        assertNull(consumerTaskNameFor("commonTest"), "commonTest is owned by the common producer")
        assertNull(consumerTaskNameFor("jsMain"))
        assertNull(consumerTaskNameFor("Test"))
    }

    private fun routeKmp(compilationName: String, platformTypeName: String): CacheCorrectDecision =
        routeCompilation(
                hasKotlinSourceSetModel = true,
                isMultiplatform = true,
                hasCommonMainProducer = true,
                compilationName = compilationName,
                platformTypeName = platformTypeName,
            )
            .decision
}
