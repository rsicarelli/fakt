// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.builder

import com.rsicarelli.fakt.codegen.model.CodeType
import com.rsicarelli.fakt.codegen.renderer.render
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for type parsing.
 *
 * Tests parseType() function which converts string type representations
 * into structured CodeType instances.
 */
class TypeParsingTest {

    // ========================================
    // Simple Types
    // ========================================

    @Test
    fun `GIVEN simple type String WHEN parsing THEN returns Simple CodeType`() {
        // GIVEN
        val typeString = "String"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Simple)
        assertEquals("String", result.name)
    }

    @Test
    fun `GIVEN simple type Int WHEN parsing THEN returns Simple CodeType`() {
        // GIVEN
        val typeString = "Int"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Simple)
        assertEquals("Int", (result as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN custom type User WHEN parsing THEN returns Simple CodeType`() {
        // GIVEN
        val typeString = "User"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Simple)
        assertEquals("User", (result as CodeType.Simple).name)
    }

    // ========================================
    // Nullable Types
    // ========================================

    @Test
    fun `GIVEN nullable String WHEN parsing THEN returns Nullable CodeType`() {
        // GIVEN
        val typeString = "String?"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Nullable)
        val inner = (result as CodeType.Nullable).inner
        assertTrue(inner is CodeType.Simple)
        assertEquals("String", (inner as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN nullable User WHEN parsing THEN returns Nullable CodeType`() {
        // GIVEN
        val typeString = "User?"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Nullable)
        val inner = (result as CodeType.Nullable).inner
        assertTrue(inner is CodeType.Simple)
        assertEquals("User", (inner as CodeType.Simple).name)
    }

    // ========================================
    // Generic Types - Single Argument
    // ========================================

    @Test
    fun `GIVEN List of String WHEN parsing THEN returns Generic CodeType`() {
        // GIVEN
        val typeString = "List<String>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val generic = result as CodeType.Generic
        assertEquals("List", generic.name)
        assertEquals(1, generic.arguments.size)
        assertTrue(generic.arguments[0] is CodeType.Simple)
        assertEquals("String", (generic.arguments[0] as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN Set of Int WHEN parsing THEN returns Generic CodeType`() {
        // GIVEN
        val typeString = "Set<Int>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val generic = result as CodeType.Generic
        assertEquals("Set", generic.name)
        assertEquals(1, generic.arguments.size)
        assertEquals("Int", (generic.arguments[0] as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN Array of User WHEN parsing THEN returns Generic CodeType`() {
        // GIVEN
        val typeString = "Array<User>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val generic = result as CodeType.Generic
        assertEquals("Array", generic.name)
        assertEquals(1, generic.arguments.size)
        assertEquals("User", (generic.arguments[0] as CodeType.Simple).name)
    }

    // ========================================
    // Generic Types - Multiple Arguments
    // ========================================

    @Test
    fun `GIVEN Map of String to Int WHEN parsing THEN returns Generic with two arguments`() {
        // GIVEN
        val typeString = "Map<String, Int>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val generic = result as CodeType.Generic
        assertEquals("Map", generic.name)
        assertEquals(2, generic.arguments.size)
        assertEquals("String", (generic.arguments[0] as CodeType.Simple).name)
        assertEquals("Int", (generic.arguments[1] as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN Pair of String and User WHEN parsing THEN returns Generic with two arguments`() {
        // GIVEN
        val typeString = "Pair<String, User>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val generic = result as CodeType.Generic
        assertEquals("Pair", generic.name)
        assertEquals(2, generic.arguments.size)
        assertEquals("String", (generic.arguments[0] as CodeType.Simple).name)
        assertEquals("User", (generic.arguments[1] as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN Triple of A B C WHEN parsing THEN returns Generic with three arguments`() {
        // GIVEN
        val typeString = "Triple<A, B, C>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val generic = result as CodeType.Generic
        assertEquals("Triple", generic.name)
        assertEquals(3, generic.arguments.size)
        assertEquals("A", (generic.arguments[0] as CodeType.Simple).name)
        assertEquals("B", (generic.arguments[1] as CodeType.Simple).name)
        assertEquals("C", (generic.arguments[2] as CodeType.Simple).name)
    }

    // ========================================
    // Nested Generics
    // ========================================

    @Test
    fun `GIVEN List of List of String WHEN parsing THEN returns nested Generic`() {
        // GIVEN
        val typeString = "List<List<String>>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val outer = result as CodeType.Generic
        assertEquals("List", outer.name)
        assertEquals(1, outer.arguments.size)

        val inner = outer.arguments[0]
        assertTrue(inner is CodeType.Generic)
        assertEquals("List", (inner as CodeType.Generic).name)
        assertEquals(1, inner.arguments.size)
        assertEquals("String", (inner.arguments[0] as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN Map of String to List of User WHEN parsing THEN returns nested Generic`() {
        // GIVEN
        val typeString = "Map<String, List<User>>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val map = result as CodeType.Generic
        assertEquals("Map", map.name)
        assertEquals(2, map.arguments.size)

        // First argument: String
        assertEquals("String", (map.arguments[0] as CodeType.Simple).name)

        // Second argument: List<User>
        val listArg = map.arguments[1]
        assertTrue(listArg is CodeType.Generic)
        assertEquals("List", (listArg as CodeType.Generic).name)
        assertEquals("User", (listArg.arguments[0] as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN List of Map of String to Int WHEN parsing THEN returns nested Generic`() {
        // GIVEN
        val typeString = "List<Map<String, Int>>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val list = result as CodeType.Generic
        assertEquals("List", list.name)
        assertEquals(1, list.arguments.size)

        val mapArg = list.arguments[0]
        assertTrue(mapArg is CodeType.Generic)
        val map = mapArg as CodeType.Generic
        assertEquals("Map", map.name)
        assertEquals(2, map.arguments.size)
        assertEquals("String", (map.arguments[0] as CodeType.Simple).name)
        assertEquals("Int", (map.arguments[1] as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN deeply nested generics WHEN parsing THEN returns correct structure`() {
        // GIVEN
        val typeString = "Map<String, List<Map<Int, User>>>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val outerMap = result as CodeType.Generic
        assertEquals("Map", outerMap.name)
        assertEquals(2, outerMap.arguments.size)

        // First arg: String
        assertEquals("String", (outerMap.arguments[0] as CodeType.Simple).name)

        // Second arg: List<Map<Int, User>>
        val listArg = outerMap.arguments[1] as CodeType.Generic
        assertEquals("List", listArg.name)

        // List's arg: Map<Int, User>
        val innerMap = listArg.arguments[0] as CodeType.Generic
        assertEquals("Map", innerMap.name)
        assertEquals("Int", (innerMap.arguments[0] as CodeType.Simple).name)
        assertEquals("User", (innerMap.arguments[1] as CodeType.Simple).name)
    }

    // ========================================
    // Nullable Generics
    // ========================================

    @Test
    fun `GIVEN nullable List of String WHEN parsing THEN returns Nullable Generic`() {
        // GIVEN
        val typeString = "List<String>?"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Nullable)
        val inner = (result as CodeType.Nullable).inner
        assertTrue(inner is CodeType.Generic)
        val generic = inner as CodeType.Generic
        assertEquals("List", generic.name)
        assertEquals("String", (generic.arguments[0] as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN List of nullable String WHEN parsing THEN returns Generic with Nullable argument`() {
        // GIVEN
        val typeString = "List<String?>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val generic = result as CodeType.Generic
        assertEquals("List", generic.name)

        val arg = generic.arguments[0]
        assertTrue(arg is CodeType.Nullable)
        assertEquals("String", ((arg as CodeType.Nullable).inner as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN nullable Map of String to nullable User WHEN parsing THEN returns complex nullable structure`() {
        // GIVEN
        val typeString = "Map<String, User?>?"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Nullable)
        val map = (result as CodeType.Nullable).inner as CodeType.Generic
        assertEquals("Map", map.name)

        assertEquals("String", (map.arguments[0] as CodeType.Simple).name)

        val userArg = map.arguments[1]
        assertTrue(userArg is CodeType.Nullable)
        assertEquals("User", ((userArg as CodeType.Nullable).inner as CodeType.Simple).name)
    }

    // ========================================
    // Edge Cases
    // ========================================

    @Test
    fun `GIVEN type with spaces WHEN parsing THEN handles spaces correctly`() {
        // GIVEN
        val typeString = "Map<String, Int>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val generic = result as CodeType.Generic
        assertEquals("Map", generic.name)
        assertEquals(2, generic.arguments.size)
        // Note: spaces are preserved in simple names if present
        assertEquals("String", (generic.arguments[0] as CodeType.Simple).name)
        assertEquals("Int", (generic.arguments[1] as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN fully qualified type name WHEN parsing THEN preserves full name`() {
        // GIVEN
        val typeString = "com.example.User"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Simple)
        assertEquals("com.example.User", (result as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN generic with fully qualified names WHEN parsing THEN preserves full names`() {
        // GIVEN
        val typeString = "kotlin.collections.List<com.example.User>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val generic = result as CodeType.Generic
        assertEquals("kotlin.collections.List", generic.name)
        assertEquals("com.example.User", (generic.arguments[0] as CodeType.Simple).name)
    }

    // ========================================
    // Real-world Complex Types
    // ========================================

    @Test
    fun `GIVEN StateFlow of List of User WHEN parsing THEN returns correct structure`() {
        // GIVEN
        val typeString = "StateFlow<List<User>>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val stateFlow = result as CodeType.Generic
        assertEquals("StateFlow", stateFlow.name)

        val listArg = stateFlow.arguments[0] as CodeType.Generic
        assertEquals("List", listArg.name)
        assertEquals("User", (listArg.arguments[0] as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN Result of nullable User WHEN parsing THEN returns correct structure`() {
        // GIVEN
        val typeString = "Result<User?>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val resultType = result as CodeType.Generic
        assertEquals("Result", resultType.name)

        val userArg = resultType.arguments[0]
        assertTrue(userArg is CodeType.Nullable)
        assertEquals("User", ((userArg as CodeType.Nullable).inner as CodeType.Simple).name)
    }

    @Test
    fun `GIVEN Flow of Pair of String and Int WHEN parsing THEN returns correct structure`() {
        // GIVEN
        val typeString = "Flow<Pair<String, Int>>"

        // WHEN
        val result = parseType(typeString)

        // THEN
        assertTrue(result is CodeType.Generic)
        val flow = result as CodeType.Generic
        assertEquals("Flow", flow.name)

        val pairArg = flow.arguments[0] as CodeType.Generic
        assertEquals("Pair", pairArg.name)
        assertEquals("String", (pairArg.arguments[0] as CodeType.Simple).name)
        assertEquals("Int", (pairArg.arguments[1] as CodeType.Simple).name)
    }

    // ========================================
    // Type Rendering Round-trip
    // ========================================

    @Test
    fun `GIVEN parsed simple type WHEN rendering THEN returns original string`() {
        // GIVEN
        val original = "User"
        val parsed = parseType(original)

        // WHEN
        val rendered = parsed.render()

        // THEN
        assertEquals(original, rendered)
    }

    @Test
    fun `GIVEN parsed nullable type WHEN rendering THEN returns original string`() {
        // GIVEN
        val original = "User?"
        val parsed = parseType(original)

        // WHEN
        val rendered = parsed.render()

        // THEN
        assertEquals(original, rendered)
    }

    @Test
    fun `GIVEN parsed generic type WHEN rendering THEN returns original string`() {
        // GIVEN
        val original = "List<String>"
        val parsed = parseType(original)

        // WHEN
        val rendered = parsed.render()

        // THEN
        assertEquals(original, rendered)
    }

    @Test
    fun `GIVEN parsed complex nested type WHEN rendering THEN returns original string`() {
        // GIVEN
        val original = "Map<String, List<User>>"
        val parsed = parseType(original)

        // WHEN
        val rendered = parsed.render()

        // THEN
        assertEquals(original, rendered)
    }

    @Test
    fun `GIVEN parsed nullable generic WHEN rendering THEN returns original string`() {
        // GIVEN
        val original = "List<String?>?"
        val parsed = parseType(original)

        // WHEN
        val rendered = parsed.render()

        // THEN
        assertEquals(original, rendered)
    }
}
