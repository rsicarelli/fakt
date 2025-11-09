// Copyright (C) 2025 Rodrigo Sicarelli.
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.codegen.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * Tests for code model immutability and structure.
 */
class CodeModelTest {

    @Test
    fun `GIVEN CodeFile WHEN adding declaration THEN returns new instance with preserved immutability`() {
        // GIVEN
        val file = CodeFile(packageName = "com.example")
        val decl = CodeClass(name = "Test")

        // WHEN
        val updated = file.addDeclaration(decl)

        // THEN - Immutability
        assertNotSame(file, updated)
        assertEquals(0, file.declarations.size)
        assertEquals(1, updated.declarations.size)
    }

    @Test
    fun `GIVEN CodeFile WHEN adding imports THEN deduplicates and preserves order`() {
        // GIVEN
        val file = CodeFile("com.example")

        // WHEN
        val updated = file.addImport("kotlin.collections.List")
            .addImport("com.example.User")
            .addImport("kotlin.collections.List") // Duplicate

        // THEN
        assertEquals(2, updated.imports.size)
        assertTrue(updated.imports.contains("kotlin.collections.List"))
        assertTrue(updated.imports.contains("com.example.User"))
    }

    @Test
    fun `GIVEN CodeClass WHEN adding members THEN returns new instance with members`() {
        // GIVEN
        val clazz = CodeClass(name = "MyClass")
        val property = CodeProperty(name = "value", type = CodeType.Simple("String"))

        // WHEN
        val updated = clazz.addMember(property)

        // THEN - Immutability
        assertEquals(0, clazz.members.size)
        assertEquals(1, updated.members.size)
    }

    @Test
    fun `GIVEN CodeType WHEN creating different types THEN represents correctly`() {
        // Simple type
        val simple = CodeType.Simple("String")
        assertTrue(simple is CodeType.Simple)
        assertEquals("String", simple.name)

        // Generic type
        val generic = CodeType.Generic("List", listOf(CodeType.Simple("String")))
        assertTrue(generic is CodeType.Generic)
        assertEquals("List", generic.name)
        assertEquals(1, generic.arguments.size)

        // Nullable type
        val nullable = CodeType.Nullable(CodeType.Simple("String"))
        assertTrue(nullable is CodeType.Nullable)

        // Lambda type
        val lambda = CodeType.Lambda(
            parameters = listOf(CodeType.Simple("String")),
            returnType = CodeType.Simple("Int"),
            isSuspend = false
        )
        assertTrue(lambda is CodeType.Lambda)
        assertFalse(lambda.isSuspend)
    }

    @Test
    fun `GIVEN CodeFunction WHEN creating with parameters THEN structure is correct`() {
        // GIVEN
        val function = CodeFunction(
            name = "findById",
            parameters = listOf(
                CodeParameter("id", CodeType.Simple("String"))
            ),
            returnType = CodeType.Nullable(CodeType.Simple("User")),
            body = CodeBlock.Statements(listOf("return null")),
            modifiers = setOf(CodeModifier.OVERRIDE)
        )

        // THEN
        assertEquals("findById", function.name)
        assertEquals(1, function.parameters.size)
        assertEquals("id", function.parameters[0].name)
        assertTrue(function.returnType is CodeType.Nullable)
        assertTrue(CodeModifier.OVERRIDE in function.modifiers)
    }

    @Test
    fun `GIVEN CodeProperty WHEN creating mutable property THEN structure is correct`() {
        // GIVEN
        val property = CodeProperty(
            name = "count",
            type = CodeType.Simple("Int"),
            modifiers = setOf(CodeModifier.PRIVATE),
            initializer = CodeExpression.NumberLiteral("0"),
            isMutable = true
        )

        // THEN
        assertEquals("count", property.name)
        assertTrue(property.isMutable)
        assertTrue(CodeModifier.PRIVATE in property.modifiers)
        assertTrue(property.initializer is CodeExpression.NumberLiteral)
    }
}
