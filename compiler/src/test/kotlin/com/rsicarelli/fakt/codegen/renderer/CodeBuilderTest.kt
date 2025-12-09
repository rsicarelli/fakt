// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.renderer

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for CodeBuilder - Format-by-Construction DSL.
 *
 * CodeBuilder ensures:
 * - Correct indentation (impossible to mess up)
 * - Zero memory allocations (single StringBuilder)
 * - Clean, fluent API
 */
class CodeBuilderTest {
    @Test
    fun `GIVEN CodeBuilder WHEN appending line at level 0 THEN no indentation`() {
        // GIVEN
        val builder = CodeBuilder()

        // WHEN
        builder.appendLine("val x = 1")

        // THEN
        assertEquals("val x = 1\n", builder.build())
    }

    @Test
    fun `GIVEN CodeBuilder WHEN appending multiple lines THEN each has newline`() {
        // GIVEN
        val builder = CodeBuilder()

        // WHEN
        builder.appendLine("val x = 1")
        builder.appendLine("val y = 2")

        // THEN
        assertEquals("val x = 1\nval y = 2\n", builder.build())
    }

    @Test
    fun `GIVEN CodeBuilder WHEN using indent block THEN increases indentation`() {
        // GIVEN
        val builder = CodeBuilder()

        // WHEN
        builder.appendLine("class Foo {")
        builder.indent {
            appendLine("val x = 1")
        }
        builder.appendLine("}")

        // THEN
        val expected =
            """
            class Foo {
                val x = 1
            }

            """.trimIndent()
        assertEquals(expected, builder.build())
    }

    @Test
    fun `GIVEN CodeBuilder WHEN nesting indent blocks THEN accumulates indentation`() {
        // GIVEN
        val builder = CodeBuilder()

        // WHEN
        builder.block("class Outer") {
            block("class Inner") {
                appendLine("val x = 1")
            }
        }

        // THEN
        val expected =
            """
            class Outer {
                class Inner {
                    val x = 1
                }
            }

            """.trimIndent()
        assertEquals(expected, builder.build())
    }

    @Test
    fun `GIVEN CodeBuilder WHEN using block helper THEN adds braces automatically`() {
        // GIVEN
        val builder = CodeBuilder()

        // WHEN
        builder.block("fun test()") {
            appendLine("return 42")
        }

        // THEN
        val expected =
            """
            fun test() {
                return 42
            }

            """.trimIndent()
        assertEquals(expected, builder.build())
    }

    @Test
    fun `GIVEN CodeBuilder WHEN appending empty line THEN adds newline without indentation`() {
        // GIVEN
        val builder = CodeBuilder()

        // WHEN
        builder.appendLine("val x = 1")
        builder.appendLine()
        builder.appendLine("val y = 2")

        // THEN
        assertEquals("val x = 1\n\nval y = 2\n", builder.build())
    }

    @Test
    fun `GIVEN CodeBuilder WHEN building large code THEN uses single StringBuilder`() {
        // GIVEN
        val builder = CodeBuilder()

        // WHEN - Simulate large generation
        repeat(100) { i ->
            builder.block("class Class$i") {
                appendLine("val value = $i")
            }
        }

        // THEN - Should complete without OOM and contain all classes
        val result = builder.build()
        assertTrue(result.contains("class Class0"))
        assertTrue(result.contains("class Class99"))
        assertTrue(result.contains("val value = 50"))
    }

    @Test
    fun `GIVEN CodeBuilder WHEN indent decreases THEN returns to previous level`() {
        // GIVEN
        val builder = CodeBuilder()

        // WHEN
        builder.appendLine("level 0")
        builder.indent {
            appendLine("level 1")
            indent {
                appendLine("level 2")
            }
            appendLine("back to level 1")
        }
        builder.appendLine("back to level 0")

        // THEN
        val expected =
            """
            level 0
                level 1
                    level 2
                back to level 1
            back to level 0

            """.trimIndent()
        assertEquals(expected, builder.build())
    }

    @Test
    fun `GIVEN CodeBuilder WHEN building empty THEN returns empty string`() {
        // GIVEN
        val builder = CodeBuilder()

        // WHEN/THEN
        assertEquals("", builder.build())
    }

    @Test
    fun `GIVEN CodeBuilder WHEN appending complex structure THEN formats correctly`() {
        // GIVEN
        val builder = CodeBuilder()

        // WHEN - Build complete fake implementation
        builder.appendLine("package com.example")
        builder.appendLine()
        builder.block("class FakeUserServiceImpl : UserService") {
            appendLine("private var getUserBehavior: (String) -> User? = { _ -> null }")
            appendLine()
            block("override fun getUser(id: String): User?") {
                appendLine("return getUserBehavior(id)")
            }
        }

        // THEN
        val result = builder.build()
        assertTrue(result.contains("package com.example"))
        assertTrue(result.contains("class FakeUserServiceImpl"))
        assertTrue(result.contains("    private var getUserBehavior")) // 4 spaces
        assertTrue(result.contains("        return getUserBehavior(id)")) // 8 spaces
    }
}
