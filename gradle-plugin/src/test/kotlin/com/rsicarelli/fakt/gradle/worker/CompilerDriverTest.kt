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
}
