// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.renderer

/**
 * Zero-dependency code builder with automatic indentation management.
 *
 * Implements the "Format-by-Construction" pattern.
 * Single StringBuilder instance passed through entire generation for zero allocations.
 *
 * Key features:
 * - Automatic indentation tracking
 * - Impossible to mess up indentation (managed by API)
 * - Zero memory allocations (single StringBuilder)
 * - Fluent, composable API
 *
 * Example:
 * ```kotlin
 * val builder = CodeBuilder()
 * builder.block("class Foo") {
 *     appendLine("val x = 1")
 *     block("fun test()") {
 *         appendLine("return x")
 *     }
 * }
 * val code = builder.build()
 * ```
 *
 * @property builder The underlying StringBuilder (single instance for entire generation)
 * @property indentLevel Current indentation level (incremented by indent blocks)
 * @property indentSize Number of spaces per indentation level (default: 4)
 */
public class CodeBuilder(
    @PublishedApi
    internal val builder: StringBuilder = StringBuilder(),
    @PublishedApi
    internal var indentLevel: Int = 0,
    public val indentSize: Int = 4
) {
    /**
     * Current indentation string (spaces per level based on indentSize).
     */
    @PublishedApi
    internal val indent: String
        get() = " ".repeat(indentSize * indentLevel)

    /**
     * Appends a line with current indentation.
     *
     * If line is blank, appends newline only (no indentation).
     *
     * @param line The line to append (default: empty)
     */
    public fun appendLine(line: String = "") {
        if (line.isBlank()) {
            builder.appendLine()
        } else {
            builder.appendLine("$indent$line")
        }
    }

    /**
     * Increases indent level for the code generated in the [body] block.
     *
     * Indent level is automatically restored after [body] executes.
     *
     * Example:
     * ```kotlin
     * builder.appendLine("class Foo {")
     * builder.indent {
     *     appendLine("val x = 1")  // Indented
     * }
     * builder.appendLine("}")  // Back to original level
     * ```
     *
     * @param body Code block to execute with increased indentation
     */
    public inline fun indent(body: CodeBuilder.() -> Unit) {
        indentLevel++
        this.body()
        indentLevel--
    }

    /**
     * Appends a line with a block opener (e.g., "class Foo {")
     * and executes [body] with increased indent.
     *
     * Automatically adds opening brace and closing brace with correct indentation.
     *
     * Example:
     * ```kotlin
     * builder.block("fun test()") {
     *     appendLine("return 42")
     * }
     * // Generates:
     * // fun test() {
     * //     return 42
     * // }
     * ```
     *
     * @param header The header line (e.g., "class Foo", "fun test()")
     * @param body Code block to execute inside the braces
     */
    public inline fun block(header: String, body: CodeBuilder.() -> Unit) {
        appendLine("$header {")
        indent(body)
        appendLine("}")
    }

    /**
     * Returns the final generated code as a string.
     *
     * This should be called once at the end of code generation.
     *
     * @return The complete generated code
     */
    public fun build(): String = builder.toString()
}
