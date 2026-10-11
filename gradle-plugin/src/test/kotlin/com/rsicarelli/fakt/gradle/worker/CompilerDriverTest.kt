// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pure unit coverage for [CompilerDriver.forPlatformType], the switch that picks the compiler front
 * door the worker drives for a `SourceSetContext.platformType`. Runs without the forked worker or
 * `kotlin-compiler-embeddable`, so a routing regression (e.g. a JS compilation handed to the K2JVM
 * driver, which cannot read klibs) fails fast here instead of only in the TestKit suites.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompilerDriverTest {

    @Test
    fun `GIVEN common platform WHEN picking the driver THEN uses the metadata compiler`() {
        assertEquals(CompilerDriver.METADATA, CompilerDriver.forPlatformType("common"))
    }

    @Test
    fun `GIVEN js platform WHEN picking the driver THEN uses K2JSCompiler`() {
        assertEquals(CompilerDriver.JS, CompilerDriver.forPlatformType("js"))
    }

    @Test
    fun `GIVEN wasm platform WHEN picking the driver THEN uses K2JSCompiler in wasm mode`() {
        assertEquals(CompilerDriver.JS, CompilerDriver.forPlatformType("wasm"))
    }

    @Test
    fun `GIVEN jvm and androidJvm platforms WHEN picking the driver THEN uses K2JVMCompiler`() {
        assertEquals(CompilerDriver.JVM, CompilerDriver.forPlatformType("jvm"))
        assertEquals(CompilerDriver.JVM, CompilerDriver.forPlatformType("androidJvm"))
    }

    @Test
    fun `GIVEN the JS driver WHEN reading its front door THEN points at K2JSCompiler and its arguments`() {
        assertEquals("org.jetbrains.kotlin.cli.js.K2JSCompiler", CompilerDriver.JS.compilerFqn)
        assertEquals(
            "org.jetbrains.kotlin.cli.common.arguments.K2JSCompilerArguments",
            CompilerDriver.JS.argumentsFqn,
        )
    }

    @Test
    fun `GIVEN native platform WHEN picking the driver THEN uses K2Native`() {
        assertEquals(CompilerDriver.NATIVE, CompilerDriver.forPlatformType("native"))
    }

    @Test
    fun `GIVEN the native driver WHEN reading its front door THEN points at K2Native and its arguments`() {
        assertEquals("org.jetbrains.kotlin.cli.bc.K2Native", CompilerDriver.NATIVE.compilerFqn)
        assertEquals(
            "org.jetbrains.kotlin.cli.common.arguments.K2NativeCompilerArguments",
            CompilerDriver.NATIVE.argumentsFqn,
        )
    }

    @Test
    fun `GIVEN a common platform with konan targets WHEN selecting THEN uses K2Native`() {
        val driver =
            CompilerDriver.select(
                platformType = "common",
                konanTargets = listOf("linux_arm64", "linux_x64"),
            )

        assertEquals(CompilerDriver.NATIVE, driver)
    }

    @Test
    fun `GIVEN a common platform without konan targets WHEN selecting THEN uses the metadata compiler`() {
        assertEquals(CompilerDriver.METADATA, CompilerDriver.select("common", emptyList()))
    }

    @Test
    fun `GIVEN no konan targets WHEN selecting THEN falls back to the platform driver`() {
        assertEquals(CompilerDriver.JVM, CompilerDriver.select("jvm", emptyList()))
        assertEquals(CompilerDriver.JS, CompilerDriver.select("js", emptyList()))
    }
}
