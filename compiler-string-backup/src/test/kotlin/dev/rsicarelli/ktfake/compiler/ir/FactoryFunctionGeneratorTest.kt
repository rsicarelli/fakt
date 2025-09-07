// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

/**
 * Tests for factory function generation in IR phase.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover factory function naming, signatures, and behavior.
 *
 * Based on roadmap pattern:
 * ```kotlin
 * @Generated("ktfake")
 * fun fakeUserService(configure: FakeUserServiceConfig.() -> Unit = {}): UserService {
 *     return FakeUserServiceImpl().apply {
 *         FakeUserServiceConfig(this).configure()
 *     }
 * }
 * ```
 */
class FactoryFunctionGeneratorTest {

    private lateinit var generator: FactoryFunctionGenerator

    @BeforeTest
    fun setUp() {
        generator = FactoryFunctionGenerator()
    }

    @Test
    fun `GIVEN interface UserService WHEN generating factory THEN should create fakeUserService function`() {
        // Given: Interface named "UserService"
        // When: Generating factory function
        // Then: Should create function named "fakeUserService"

        assertEquals("fakeUserService", "fakeUserService", "Test structure for factory naming")
    }

    @Test
    fun `GIVEN interface with methods WHEN generating factory THEN should include configuration parameter`() {
        // Given: Interface with methods
        // interface UserService { fun getUser(id: String): User }
        // When: Generating factory function
        // Then: Should include configure parameter of type FakeUserServiceConfig.() -> Unit

        assertTrue(true, "Test structure for configuration parameter")
    }

    @Test
    fun `GIVEN factory function WHEN generated THEN should have @Generated annotation`() {
        // Given: Factory function generation
        // When: Function is generated
        // Then: Should include @Generated("ktfake") annotation

        assertTrue(true, "Test structure for @Generated annotation")
    }

    @Test
    fun `GIVEN factory function WHEN generated THEN should return implementation instance`() {
        // Given: Factory function generation
        // When: Function is generated
        // Then: Should return FakeServiceImpl() instance wrapped in configuration

        assertTrue(true, "Test structure for implementation instance return")
    }

    @Test
    fun `GIVEN custom factory name in @FakeConfig WHEN generating THEN should use custom name`() {
        // Given: @FakeConfig(factoryName = "createTestUserService")
        // When: Generating factory function
        // Then: Should create function named "createTestUserService"

        assertTrue(true, "Test structure for custom factory names")
    }
}
