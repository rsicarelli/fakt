// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import org.junit.jupiter.api.TestInstance

/**
 * Tests for factory function generation.
 *
 * Validates that the factory delegates to config.build() and does not directly access internal
 * config properties (which would break inline/internal visibility rules).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FactoryExtensionsTest {

    // ==========================================
    // Factory Body — Delegates to build()
    // ==========================================

    @Test
    fun `GIVEN interface with methods WHEN generating factory THEN delegates to config build`() {
        // GIVEN
        val spec =
            FactoryFunctionSpec(
                interfaceName = "UserService",
                methods =
                    listOf(
                        MethodSpec(
                            name = "getUser",
                            params = listOf(Triple("id", "String", false)),
                            returnType = "User?",
                            defaultBehavior = "{ p0 -> null }",
                        )
                    ),
            )

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN
        assertContains(result, "FakeUserServiceConfig().apply(configure).build()")
    }

    @Test
    fun `GIVEN interface with properties WHEN generating factory THEN delegates to config build`() {
        // GIVEN
        val spec =
            FactoryFunctionSpec(
                interfaceName = "Counter",
                properties =
                    listOf(
                        PropertySpec(
                            name = "count",
                            type = "Int",
                            isStateFlow = false,
                            isMutable = false,
                            defaultBehavior = "{ 0 }",
                        )
                    ),
            )

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN
        assertContains(result, "FakeCounterConfig().apply(configure).build()")
    }

    @Test
    fun `GIVEN empty interface WHEN generating factory THEN delegates to config build`() {
        // GIVEN
        val spec = FactoryFunctionSpec(interfaceName = "EmptyService")

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN
        assertContains(result, "FakeEmptyServiceConfig().apply(configure).build()")
        assertFalse(result.contains("val config"), "Should use expression body, not block body")
    }

    // ==========================================
    // Factory Does Not Access Internal Properties
    // ==========================================

    @Test
    fun `GIVEN factory WHEN generating THEN does not directly access config internal properties`() {
        // GIVEN
        val spec =
            FactoryFunctionSpec(
                interfaceName = "Service",
                methods =
                    listOf(
                        MethodSpec(
                            name = "doWork",
                            params = emptyList(),
                            returnType = "Unit",
                            defaultBehavior = "{ Unit }",
                        )
                    ),
                properties =
                    listOf(
                        PropertySpec(
                            name = "count",
                            type = "Int",
                            isStateFlow = false,
                            defaultBehavior = "{ 0 }",
                        )
                    ),
            )

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN - Factory should NOT directly access internal config properties
        assertFalse(
            result.contains("config."),
            "Factory should delegate to build() instead of accessing config properties",
        )
        assertFalse(
            result.contains("?:"),
            "Factory should not contain null-coalescing (handled by build)",
        )
    }

    // ==========================================
    // Factory Structure
    // ==========================================

    @Test
    fun `GIVEN interface WHEN generating factory THEN creates inline function with config DSL param`() {
        // GIVEN
        val spec =
            FactoryFunctionSpec(
                interfaceName = "UserService",
                methods =
                    listOf(
                        MethodSpec(
                            name = "getUser",
                            params = emptyList(),
                            returnType = "String",
                            defaultBehavior = "{ \"\" }",
                        )
                    ),
            )

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN
        assertContains(result, "inline fun fakeUserService")
        assertContains(result, "configure: FakeUserServiceConfig.() -> Unit = {}")
        assertContains(result, ".apply(configure).build()")
    }

    // ==========================================
    // Visibility
    // ==========================================

    @Test
    fun `GIVEN internal visibility WHEN generating factory THEN function is internal`() {
        // GIVEN
        val spec =
            FactoryFunctionSpec(
                interfaceName = "InternalService",
                visibility = com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility.INTERNAL,
            )

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN
        assertContains(result, "internal inline fun fakeInternalService")
    }
}
