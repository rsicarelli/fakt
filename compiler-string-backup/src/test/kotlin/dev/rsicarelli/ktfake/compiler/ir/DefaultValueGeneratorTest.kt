// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.ir

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.BeforeTest

/**
 * Tests for default value generation in builder pattern.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover unique value generation, type-specific defaults, and sensible fallbacks.
 *
 * Based on requirements:
 * - Default values should be sensible and unique
 * - String IDs should use UUID-based generation
 * - Numeric types should have reasonable defaults
 * - Complex types should use nested fakes
 */
class DefaultValueGeneratorTest {

    private lateinit var generator: DefaultValueGenerator

    @BeforeTest
    fun setUp() {
        generator = DefaultValueGenerator()
    }

    @Test
    fun `GIVEN String property WHEN generating default THEN should create unique string with Kotlin UUID`() {
        // Given: String property that should be unique
        // When: Generating default value
        val default1 = generator.generateDefaultValue("String", "id")
        val default2 = generator.generateDefaultValue("String", "id")

        // Then: Should generate unique string values using official Kotlin UUID
        assertTrue(default1.startsWith("\"fake_"), "Should start with fake_ prefix")
        assertTrue(default1.contains("-"), "Should contain UUID structure")
        assertNotEquals(default1, default2, "Should generate unique values each time")
    }

    @Test
    fun `GIVEN numeric properties WHEN generating defaults THEN should provide sensible values`() {
        // Given: Various numeric property types
        // When: Generating default values
        val intDefault = generator.generateDefaultValue("Int", "count")
        val longDefault = generator.generateDefaultValue("Long", "timestamp")
        val doubleDefault = generator.generateDefaultValue("Double", "price")
        val floatDefault = generator.generateDefaultValue("Float", "rating")

        // Then: Should generate sensible numeric defaults
        assertEquals("0", intDefault, "Int should default to 0")
        assertEquals("0L", longDefault, "Long should default to 0L")
        assertEquals("0.0", doubleDefault, "Double should default to 0.0")
        assertEquals("0.0f", floatDefault, "Float should default to 0.0f")
    }

    @Test
    fun `GIVEN Boolean property WHEN generating default THEN should default to true`() {
        // Given: Boolean property
        // When: Generating default value
        val booleanDefault = generator.generateDefaultValue("Boolean", "isActive")

        // Then: Should default to true (more useful for testing)
        assertEquals("true", booleanDefault, "Boolean should default to true")
    }

    @Test
    fun `GIVEN custom class properties WHEN generating defaults THEN should use nested fake functions`() {
        // Given: Custom class properties
        // When: Generating default values
        val userDefault = generator.generateDefaultValue("User", "owner")
        val preferencesDefault = generator.generateDefaultValue("UserPreferences", "preferences")
        val settingsDefault = generator.generateDefaultValue("AppSettings", "settings")

        // Then: Should generate nested fake function calls
        assertEquals("fakeUser()", userDefault, "Should use fakeUser() for User type")
        assertEquals("fakeUserPreferences()", preferencesDefault, "Should use fakeUserPreferences()")
        assertEquals("fakeAppSettings()", settingsDefault, "Should use fakeAppSettings()")
    }

    @Test
    fun `GIVEN collection properties WHEN generating defaults THEN should create empty collections`() {
        // Given: Various collection types
        // When: Generating default values
        val listDefault = generator.generateDefaultValue("List<String>", "items")
        val setDefault = generator.generateDefaultValue("Set<Int>", "numbers")
        val mapDefault = generator.generateDefaultValue("Map<String, Any>", "metadata")

        // Then: Should generate empty collections
        assertEquals("emptyList()", listDefault, "List should default to emptyList()")
        assertEquals("emptySet()", setDefault, "Set should default to emptySet()")
        assertEquals("emptyMap()", mapDefault, "Map should default to emptyMap()")
    }

    @Test
    fun `GIVEN nullable properties WHEN generating defaults THEN should default to null`() {
        // Given: Nullable property types
        // When: Generating default values
        val nullableStringDefault = generator.generateDefaultValue("String?", "optionalName")
        val nullableUserDefault = generator.generateDefaultValue("User?", "optionalUser")

        // Then: Should default nullable types to null
        assertEquals("null", nullableStringDefault, "Nullable String should default to null")
        assertEquals("null", nullableUserDefault, "Nullable custom type should default to null")
    }

    @Test
    fun `GIVEN property names WHEN generating context-aware defaults THEN should use meaningful values`() {
        // Given: Properties with contextual names
        // When: Generating context-aware defaults
        val emailDefault = generator.generateContextAwareDefault("String", "email")
        val nameDefault = generator.generateContextAwareDefault("String", "name")
        val ageDefault = generator.generateContextAwareDefault("Int", "age")

        // Then: Should generate contextually appropriate defaults
        assertTrue(emailDefault.contains("@"), "Email should contain @ symbol")
        assertTrue(nameDefault.contains("Fake"), "Name should contain 'Fake' prefix")
        assertTrue(ageDefault.toInt() > 0, "Age should be positive number")
    }

    @Test
    fun `GIVEN enum properties WHEN generating defaults THEN should use first enum value`() {
        // Given: Enum property types
        // When: Generating default values
        val statusDefault = generator.generateDefaultValue("Status", "status", enumValues = listOf("ACTIVE", "INACTIVE"))
        val priorityDefault = generator.generateDefaultValue("Priority", "priority", enumValues = listOf("HIGH", "MEDIUM", "LOW"))

        // Then: Should use first enum value
        assertEquals("Status.ACTIVE", statusDefault, "Should use first enum value")
        assertEquals("Priority.HIGH", priorityDefault, "Should use first enum value")
    }

    @Test
    fun `GIVEN unique ID requirements WHEN generating unique strings THEN should create valid Kotlin UUID format`() {
        // Given: Need for unique string generation
        // When: Generating strings with official Kotlin UUID
        val uniqueId = generator.generateUniqueString("user")

        // Then: Should create valid Kotlin UUID format
        assertTrue(uniqueId.startsWith("\"fake_user_"), "Should start with contextual prefix")
        assertTrue(uniqueId.contains("-"), "Should contain UUID separators")
        assertTrue(uniqueId.length > 40, "Should be long enough to contain full UUID")
        assertTrue(uniqueId.endsWith("\""), "Should be properly quoted string")
    }

    @Test
    fun `GIVEN nested generic types WHEN generating defaults THEN should handle complex generics`() {
        // Given: Complex nested generic types
        // When: Generating defaults for complex generics
        val listOfUsersDefault = generator.generateDefaultValue("List<User>", "users")
        val mapOfUserListsDefault = generator.generateDefaultValue("Map<String, List<User>>", "userGroups")

        // Then: Should handle nested generics appropriately
        assertEquals("emptyList()", listOfUsersDefault, "Complex list should default to empty")
        assertEquals("emptyMap()", mapOfUserListsDefault, "Complex map should default to empty")
    }

    @Test
    fun `GIVEN type inference WHEN analyzing property types THEN should correctly parse type information`() {
        // Given: Various property type strings
        // When: Parsing type information
        val stringType = generator.parseTypeInfo("String")
        val nullableType = generator.parseTypeInfo("User?")
        val genericType = generator.parseTypeInfo("List<String>")

        // Then: Should correctly identify type characteristics
        assertEquals("String", stringType.baseType, "Should extract base type")
        assertTrue(nullableType.isNullable, "Should detect nullable types")
        assertEquals("List", genericType.baseType, "Should extract generic base type")
        assertEquals(listOf("String"), genericType.genericParams, "Should extract generic parameters")
    }
}
