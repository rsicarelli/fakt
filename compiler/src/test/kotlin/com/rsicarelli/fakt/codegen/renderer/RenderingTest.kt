// Copyright (C) 2025 Rodrigo Sicarelli.
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.codegen.renderer

import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.model.CodeBlock
import com.rsicarelli.fakt.codegen.model.CodeClass
import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeFunction
import com.rsicarelli.fakt.codegen.model.CodeModifier
import com.rsicarelli.fakt.codegen.model.CodeParameter
import com.rsicarelli.fakt.codegen.model.CodeProperty
import com.rsicarelli.fakt.codegen.model.CodeType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for code rendering (Model → String).
 *
 * Rendering converts our model to actual Kotlin source code.
 */
class RenderingTest {

    @Test
    fun `GIVEN Simple CodeType WHEN rendering THEN returns name`() {
        // GIVEN
        val type = CodeType.Simple("String")

        // WHEN
        val rendered = type.render()

        // THEN
        assertEquals("String", rendered)
    }

    @Test
    fun `GIVEN Generic CodeType WHEN rendering THEN formats with arguments`() {
        // GIVEN
        val type = CodeType.Generic(
            "List",
            listOf(CodeType.Simple("String"))
        )

        // WHEN
        val rendered = type.render()

        // THEN
        assertEquals("List<String>", rendered)
    }

    @Test
    fun `GIVEN Nullable CodeType WHEN rendering THEN adds question mark`() {
        // GIVEN
        val type = CodeType.Nullable(CodeType.Simple("User"))

        // WHEN
        val rendered = type.render()

        // THEN
        assertEquals("User?", rendered)
    }

    @Test
    fun `GIVEN Lambda CodeType WHEN rendering THEN formats function type`() {
        // GIVEN
        val type = CodeType.Lambda(
            parameters = listOf(CodeType.Simple("String")),
            returnType = CodeType.Simple("Int"),
            isSuspend = false
        )

        // WHEN
        val rendered = type.render()

        // THEN
        assertEquals("(String) -> Int", rendered)
    }

    @Test
    fun `GIVEN suspend Lambda WHEN rendering THEN includes suspend modifier`() {
        // GIVEN
        val type = CodeType.Lambda(
            parameters = listOf(CodeType.Simple("User")),
            returnType = CodeType.Simple("Unit"),
            isSuspend = true
        )

        // WHEN
        val rendered = type.render()

        // THEN
        assertEquals("suspend (User) -> Unit", rendered)
    }

    @Test
    fun `GIVEN simple property WHEN rendering THEN formats correctly`() {
        // GIVEN
        val property = CodeProperty(
            name = "value",
            type = CodeType.Simple("String"),
            initializer = CodeExpression.StringLiteral("")
        )
        val builder = CodeBuilder()

        // WHEN
        property.renderTo(builder)

        // THEN
        assertEquals("val value: String = \"\"\n", builder.build())
    }

    @Test
    fun `GIVEN private mutable property WHEN rendering THEN includes modifiers`() {
        // GIVEN
        val property = CodeProperty(
            name = "count",
            type = CodeType.Simple("Int"),
            modifiers = setOf(CodeModifier.PRIVATE),
            initializer = CodeExpression.NumberLiteral("0"),
            isMutable = true
        )
        val builder = CodeBuilder()

        // WHEN
        property.renderTo(builder)

        // THEN
        assertEquals("private var count: Int = 0\n", builder.build())
    }

    @Test
    fun `GIVEN simple function WHEN rendering THEN formats correctly`() {
        // GIVEN
        val function = CodeFunction(
            name = "test",
            returnType = CodeType.Simple("Unit"),
            body = CodeBlock.Statements(listOf("println(\"hello\")"))
        )
        val builder = CodeBuilder()

        // WHEN
        function.renderTo(builder)

        // THEN
        val expected = """
            fun test(): Unit {
                println("hello")
            }

        """.trimIndent()
        assertEquals(expected, builder.build())
    }

    @Test
    fun `GIVEN override function WHEN rendering THEN includes override modifier`() {
        // GIVEN
        val function = CodeFunction(
            name = "getUser",
            parameters = listOf(
                CodeParameter("id", CodeType.Simple("String"))
            ),
            returnType = CodeType.Nullable(CodeType.Simple("User")),
            body = CodeBlock.Statements(listOf("return null")),
            modifiers = setOf(CodeModifier.OVERRIDE)
        )
        val builder = CodeBuilder()

        // WHEN
        function.renderTo(builder)

        // THEN
        val result = builder.build()
        assertTrue(result.contains("override fun getUser"))
        assertTrue(result.contains("id: String"))
        assertTrue(result.contains("): User?"))
    }

    @Test
    fun `GIVEN simple class WHEN rendering THEN formats structure`() {
        // GIVEN
        val clazz = CodeClass(
            name = "Simple",
            members = listOf(
                CodeProperty(
                    name = "value",
                    type = CodeType.Simple("String"),
                    initializer = CodeExpression.StringLiteral("")
                )
            )
        )
        val builder = CodeBuilder()

        // WHEN
        clazz.renderTo(builder)

        // THEN
        val expected = """
            class Simple {
                val value: String = ""
            }

        """.trimIndent()
        assertEquals(expected, builder.build())
    }

    @Test
    fun `GIVEN class with interface WHEN rendering THEN includes implements`() {
        // GIVEN
        val clazz = CodeClass(
            name = "Impl",
            superTypes = listOf(CodeType.Simple("Interface"))
        )
        val builder = CodeBuilder()

        // WHEN
        clazz.renderTo(builder)

        // THEN
        assertTrue(builder.build().contains("class Impl : Interface"))
    }

    @Test
    fun `GIVEN complete file WHEN rendering THEN produces valid Kotlin`() {
        // GIVEN
        val file = codeFile("com.example") {
            header = "Generated by Fakt"
            import("com.example.User")

            klass("Simple") {
                property("value", "String") {
                    initializer = "\"\""
                }
            }
        }
        val builder = CodeBuilder()

        // WHEN
        file.renderTo(builder)

        // THEN
        val rendered = builder.build()
        assertTrue(rendered.startsWith("// Generated by Fakt\npackage com.example"))
        assertTrue(rendered.contains("import com.example.User"))
        assertTrue(rendered.contains("class Simple"))
        assertTrue(rendered.contains("val value: String = \"\""))
    }
}
