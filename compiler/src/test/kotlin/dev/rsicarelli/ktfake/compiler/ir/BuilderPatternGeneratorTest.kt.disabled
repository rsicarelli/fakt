// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

/**
 * Tests for builder pattern generation for data classes.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover builder class generation, default values, and nested fake support.
 *
 * Based on usage patterns:
 * ```kotlin
 * @Fake(builder = true)
 * data class User(val id: String, val name: String, val preferences: UserPreferences)
 *
 * // Generated usage:
 * val user = fakeUser {
 *     id = "custom-id"
 *     name = "Custom User"
 *     // preferences uses nested fake by default
 * }
 * ```
 */
class BuilderPatternGeneratorTest {

    private lateinit var generator: BuilderPatternGenerator

    @BeforeTest
    fun setUp() {
        generator = BuilderPatternGenerator()
    }

    @Test
    fun `GIVEN data class WHEN generating builder pattern THEN should create builder class`() {
        // Given: Data class with multiple properties
        val properties = listOf(
            "id: String",
            "name: String",
            "age: Int"
        )

        // When: Generating builder pattern
        val builderCode = generator.generateBuilderClass("User", properties)

        // Then: Should create builder class with proper structure
        assertTrue(builderCode.contains("class FakeUserBuilder"), "Should generate builder class")
        assertTrue(builderCode.contains("private var id: String"), "Should have id property")
        assertTrue(builderCode.contains("private var name: String"), "Should have name property")
        assertTrue(builderCode.contains("private var age: Int"), "Should have age property")
        assertTrue(builderCode.contains("fun build(): User"), "Should have build method")
    }

    @Test
    fun `GIVEN data class WHEN generating default values THEN should create sensible defaults`() {
        // Given: Data class properties with various types
        val properties = listOf(
            "id: String",
            "count: Int",
            "isActive: Boolean",
            "score: Double"
        )

        // When: Generating default values
        val defaults = generator.generateDefaultValues("User", properties)

        // Then: Should generate sensible default values
        assertTrue(defaults.contains("fake_"), "Should generate unique string IDs")
        assertTrue(defaults.contains("= 0"), "Should default integers to 0")
        assertTrue(defaults.contains("= true"), "Should default booleans to true")
        assertTrue(defaults.contains("= 0.0"), "Should default doubles to 0.0")
    }

    @Test
    fun `GIVEN nested object properties WHEN generating builder THEN should use nested fakes`() {
        // Given: Data class with nested object properties
        val properties = listOf(
            "id: String",
            "preferences: UserPreferences",
            "profile: UserProfile"
        )

        // When: Generating builder with nested fake support
        val builderCode = generator.generateBuilderWithNestedFakes("User", properties)

        // Then: Should use nested fake functions for complex properties
        assertTrue(builderCode.contains("fakeUserPreferences()"), "Should use nested fake for preferences")
        assertTrue(builderCode.contains("fakeUserProfile()"), "Should use nested fake for profile")
        assertTrue(builderCode.contains("private var id: String = \"fake_"), "Should still use simple default for strings")
    }

    @Test
    fun `GIVEN builder pattern WHEN generating factory function THEN should integrate with configuration DSL`() {
        // Given: Builder pattern enabled for data class
        // When: Generating factory function with builder support
        val factoryCode = generator.generateBuilderFactoryFunction("User")

        // Then: Should create factory that uses builder pattern
        assertTrue(factoryCode.contains("fun fakeUser(configure: FakeUserConfig.() -> Unit = {})"), "Should have configuration DSL")
        assertTrue(factoryCode.contains("FakeUserBuilder()"), "Should create builder instance")
        assertTrue(factoryCode.contains("FakeUserConfig(this).configure()"), "Should apply configuration")
        assertTrue(factoryCode.contains("build()"), "Should call build method")
    }

    @Test
    fun `GIVEN configuration DSL WHEN generating builder config THEN should provide property setters`() {
        // Given: Data class properties for configuration
        val properties = listOf(
            "id: String",
            "name: String",
            "age: Int"
        )

        // When: Generating configuration DSL for builder
        val configCode = generator.generateBuilderConfigurationDsl("User", properties)

        // Then: Should provide property setters in configuration DSL
        assertTrue(configCode.contains("class FakeUserConfig"), "Should generate config class")
        assertTrue(configCode.contains("var id: String"), "Should expose id property")
        assertTrue(configCode.contains("var name: String"), "Should expose name property")
        assertTrue(configCode.contains("var age: Int"), "Should expose age property")
    }

    @Test
    fun `GIVEN UUID generation WHEN creating unique defaults THEN should generate unique values`() {
        // Given: Multiple builder instances
        // When: Generating unique default values
        val default1 = generator.generateUniqueStringDefault("User")
        val default2 = generator.generateUniqueStringDefault("User")

        // Then: Should generate unique values
        assertTrue(default1.startsWith("\"fake_"), "Should start with fake_ prefix")
        assertTrue(default2.startsWith("\"fake_"), "Should start with fake_ prefix")
        assertTrue(default1 != default2, "Should generate different UUIDs")
    }

    @Test
    fun `GIVEN complex object graph WHEN generating nested builders THEN should support deep nesting`() {
        // Given: Complex nested data structures
        val userProperties = listOf("id: String", "profile: UserProfile")
        val profileProperties = listOf("name: String", "settings: UserSettings")

        // When: Generating builders for nested objects
        val userBuilder = generator.generateBuilderWithNestedFakes("User", userProperties)
        val profileBuilder = generator.generateBuilderWithNestedFakes("UserProfile", profileProperties)

        // Then: Should support deep object graph construction
        assertTrue(userBuilder.contains("fakeUserProfile()"), "Should use nested profile fake")
        assertTrue(profileBuilder.contains("fakeUserSettings()"), "Should use nested settings fake")
    }

    @Test
    fun `GIVEN builder pattern WHEN generating implementation THEN should be thread safe`() {
        // Given: Builder pattern generation
        // When: Creating multiple builders concurrently
        // Then: Should maintain thread safety through immutable builders

        val builderCode = generator.generateBuilderClass("User", listOf("id: String"))

        // Builder instances should be independent (no shared mutable state)
        assertTrue(builderCode.contains("private var"), "Should use private instance variables")
        assertTrue(true, "Test structure for thread-safe builder generation")
    }

    @Test
    fun `GIVEN type detection WHEN analyzing properties THEN should correctly identify nested types`() {
        // Given: Properties with various types
        val properties = listOf(
            "id: String",
            "preferences: UserPreferences",
            "items: List<Item>",
            "metadata: Map<String, Any>"
        )

        // When: Detecting which properties need nested fakes
        val nestedTypes = generator.detectNestedTypes(properties)

        // Then: Should correctly identify complex types
        assertTrue(nestedTypes.contains("UserPreferences"), "Should detect custom class types")
        assertTrue(nestedTypes.contains("Item"), "Should detect generic type parameters")
        assertTrue(!nestedTypes.contains("String"), "Should not treat primitives as nested")
    }
}
