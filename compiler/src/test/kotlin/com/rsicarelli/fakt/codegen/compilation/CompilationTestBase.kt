// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.compilation

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import kotlin.test.assertEquals

/**
 * Base class for compilation validation tests.
 *
 * Uses kotlin-compile-testing to ensure generated code actually compiles.
 * This catches type errors, syntax issues, and import problems before
 * integration with the real compiler.
 */
abstract class CompilationTestBase {

    /**
     * Asserts that the given source code compiles successfully.
     *
     * Uses kotlin-compile-testing to perform actual compilation.
     * Fails the test if compilation produces errors.
     *
     * @param sourceCode Complete Kotlin source file content
     * @param description Description of what's being compiled (for error messages)
     */
    protected fun assertCompiles(
        sourceCode: String,
        description: String = "Generated code"
    ) {
        val result = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Generated.kt", sourceCode))
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()

        assertEquals(
            KotlinCompilation.ExitCode.OK,
            result.exitCode,
            "$description failed to compile:\n${result.messages}"
        )
    }

    /**
     * Asserts that the given source code fails to compile.
     *
     * Useful for negative tests (ensuring invalid code is rejected).
     *
     * @param sourceCode Complete Kotlin source file content
     * @param description Description of what's being compiled
     */
    protected fun assertDoesNotCompile(
        sourceCode: String,
        description: String = "Generated code"
    ) {
        val result = KotlinCompilation().apply {
            sources = listOf(SourceFile.kotlin("Generated.kt", sourceCode))
            inheritClassPath = true
            messageOutputStream = System.out
        }.compile()

        assertEquals(
            KotlinCompilation.ExitCode.COMPILATION_ERROR,
            result.exitCode,
            "$description should not compile but did"
        )
    }
}
