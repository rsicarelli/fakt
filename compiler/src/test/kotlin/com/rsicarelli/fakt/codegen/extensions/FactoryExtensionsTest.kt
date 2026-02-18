// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Tests for factory function generation.
 *
 * Validates the factory expression body, config-to-constructor argument mapping, and null-coalescing
 * default resolution.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FactoryExtensionsTest {

    // ==========================================
    // Factory Body — Method Behaviors
    // ==========================================

    @Test
    fun `GIVEN single method WHEN generating factory THEN body passes config behavior with default`() {
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

        // THEN - Factory body resolves config null to default
        assertContains(
            result,
            "getUserBehavior = config.getUserBehavior ?: { p0 -> null }",
        )
    }

    @Test
    fun `GIVEN multiple methods WHEN generating factory THEN body passes all behavior args`() {
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
                        ),
                        MethodSpec(
                            name = "saveUser",
                            params = listOf(Triple("user", "User", false)),
                            returnType = "Unit",
                            isSuspend = true,
                            defaultBehavior = "{ p0 -> Unit }",
                        ),
                    ),
            )

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN - Both behaviors passed
        assertContains(result, "getUserBehavior = config.getUserBehavior ?: { p0 -> null }")
        assertContains(result, "saveUserBehavior = config.saveUserBehavior ?: { p0 -> Unit }")
    }

    @Test
    fun `GIVEN empty defaultBehavior WHEN generating factory THEN uses TODO fallback`() {
        // GIVEN
        val spec =
            FactoryFunctionSpec(
                interfaceName = "Service",
                methods =
                    listOf(
                        MethodSpec(
                            name = "doWork",
                            params = emptyList(),
                            returnType = "String",
                            defaultBehavior = "", // empty — should fallback
                        )
                    ),
            )

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN - Falls back to TODO()
        assertContains(result, "doWorkBehavior = config.doWorkBehavior ?: { TODO() }")
    }

    // ==========================================
    // Factory Body — Immutable Property Behaviors
    // ==========================================

    @Test
    fun `GIVEN immutable property WHEN generating factory THEN body passes property behavior`() {
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
        assertContains(result, "countBehavior = config.countBehavior ?: { 0 }")
    }

    // ==========================================
    // Factory Body — Mutable Property Behaviors
    // ==========================================

    @Test
    fun `GIVEN mutable property WHEN generating factory THEN body passes getter and setter`() {
        // GIVEN
        val spec =
            FactoryFunctionSpec(
                interfaceName = "UserStore",
                properties =
                    listOf(
                        PropertySpec(
                            name = "currentUser",
                            type = "User?",
                            isStateFlow = false,
                            isMutable = true,
                            defaultBehavior = "{ null }",
                        )
                    ),
            )

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN - Getter maps from config.xxxBehavior
        assertContains(
            result,
            "currentUserGetter = config.currentUserBehavior ?: { null }",
        )
        // THEN - Setter maps from config.setXxxBehavior
        assertContains(
            result,
            "currentUserSetter = config.setCurrentUserBehavior ?: { }",
        )
    }

    // ==========================================
    // Factory Body — StateFlow Properties Excluded
    // ==========================================

    @Test
    fun `GIVEN StateFlow property WHEN generating factory THEN does not include it in constructor args`() {
        // GIVEN
        val spec =
            FactoryFunctionSpec(
                interfaceName = "UserStore",
                properties =
                    listOf(
                        PropertySpec(
                            name = "users",
                            type = "StateFlow<List<User>>",
                            isStateFlow = true,
                        )
                    ),
            )

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN - StateFlow properties are NOT passed as constructor args
        assertFalse(
            result.contains("usersBehavior"),
            "StateFlow properties should not be passed as behavior constructor args",
        )
    }

    // ==========================================
    // Factory Body — Empty Interface
    // ==========================================

    @Test
    fun `GIVEN empty interface WHEN generating factory THEN uses simple construction`() {
        // GIVEN
        val spec = FactoryFunctionSpec(interfaceName = "EmptyService")

        // WHEN
        val result = generateFactoryFunction(spec, includeKDoc = false)

        // THEN - No val config needed for empty interface
        assertFalse(
            result.contains("val config"),
            "Empty interface should not use val config pattern",
        )
        assertContains(result, "FakeEmptyServiceImpl()")
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
        assertContains(result, "FakeUserServiceConfig().apply(configure)")
    }

    @Test
    fun `GIVEN methods and properties WHEN generating factory THEN passes all args in order`() {
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

        // THEN - Properties come before methods in the argument list
        val countIdx = result.indexOf("countBehavior =")
        val doWorkIdx = result.indexOf("doWorkBehavior =")
        assertTrue(
            countIdx < doWorkIdx,
            "Properties should be listed before methods in constructor args",
        )
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
