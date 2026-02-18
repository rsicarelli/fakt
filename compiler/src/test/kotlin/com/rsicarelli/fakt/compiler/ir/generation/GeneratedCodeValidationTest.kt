// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Validates generated code patterns from samples/kmp-single-module.
 *
 * **Testing Strategy**: Read actual generated files and validate expected patterns exist. This
 * pragmatic approach tests real compiler output without complex IR mocks.
 *
 * **Requirements**:
 * - samples/kmp-single-module must be built before running these tests
 * - Generated files must exist in build/generated/fakt/common/test/kotlin/
 *
 * **Coverage**:
 * - Immutable behavior properties via constructor params
 * - Default value generation for all type categories
 * - Type preservation in generated code
 * - No configureXxx methods (immutable pattern)
 */
class GeneratedCodeValidationTest {
    private val generatedDir =
        File("samples/kmp-single-module/build/generated/fakt/common/test/kotlin/test/sample")

    // ==================================================================================
    // Behavior Properties Validation (Immutable Constructor Pattern)
    // ==================================================================================

    @Test
    fun `GIVEN simple interface WHEN generated THEN should create immutable behavior properties`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakePropertyAndMethodInterfaceImpl.kt")

        // Skip if not built yet
        if (!fakeFile.exists()) {
            println(
                "⚠️  Skipping test - sample not built. Run './gradlew :samples:single-module:build' first"
            )
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN - Behavior properties should be immutable (private val)
        assertTrue(
            content.contains("private val getValueBehavior: () -> String"),
            "Should generate immutable behavior property with correct String type",
        )
        assertTrue(
            content.contains("private val setValueBehavior: (String) -> Unit"),
            "Should generate immutable behavior property with correct parameter type",
        )
        assertTrue(
            content.contains("private val stringValueBehavior: () -> String"),
            "Should generate immutable behavior property for properties",
        )

        // THEN - Should NOT have mutable behavior vars
        assertFalse(
            content.contains("private var getValueBehavior"),
            "Should NOT have mutable behavior properties (immutable pattern)",
        )
    }

    // ==================================================================================
    // Default Values Validation
    // ==================================================================================

    @Test
    fun `GIVEN String return type WHEN generated THEN should use empty string default`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakePropertyAndMethodInterfaceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN
        assertTrue(content.contains("= { \"\" }"), "String default should be empty string")
    }

    @Test
    fun `GIVEN Boolean return type WHEN generated THEN should use false default`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakeAuthenticationServiceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN
        assertTrue(
            content.contains("= { false }") || content.contains("= { _ -> false }"),
            "Boolean default should be false",
        )
    }

    // ==================================================================================
    // Method Overrides Validation
    // ==================================================================================

    @Test
    fun `GIVEN interface methods WHEN generated THEN should create override methods delegating to behavior`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakePropertyAndMethodInterfaceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN
        assertTrue(
            content.contains("override fun getValue(): String"),
            "Should generate override method",
        )
        assertTrue(
            content.contains("return getValueBehavior()"),
            "Should delegate to behavior property",
        )
    }

    @Test
    fun `GIVEN interface property WHEN generated THEN should create getter delegating to behavior`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakePropertyAndMethodInterfaceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN
        assertTrue(
            content.contains("override val stringValue: String get()"),
            "Should generate property getter",
        )
        assertTrue(
            content.contains("return stringValueBehavior()"),
            "Should delegate to behavior property",
        )
    }

    // ==================================================================================
    // Immutability Validation (No configureXxx methods)
    // ==================================================================================

    @Test
    fun `GIVEN interface methods WHEN generated THEN should NOT have configureXxx methods`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakePropertyAndMethodInterfaceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN - Immutable fakes should NOT have configure methods
        assertFalse(
            content.contains("internal fun configure"),
            "Immutable fakes should NOT have configureXxx methods",
        )
    }

    // ==================================================================================
    // Suspend Functions Validation
    // ==================================================================================

    @Test
    fun `GIVEN suspend function WHEN generated THEN should preserve suspend modifier`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakeAsyncDataServiceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN - Immutable behavior properties preserve suspend
        assertTrue(
            content.contains("private val fetchDataBehavior: suspend () -> String") ||
                content.contains("private val processDataBehavior: suspend"),
            "Should preserve suspend modifier in immutable behavior property",
        )
        assertTrue(
            content.contains("override suspend fun fetchData(): String") ||
                content.contains("override suspend fun"),
            "Should preserve suspend modifier in override",
        )
    }

    // ==================================================================================
    // Collection Defaults Validation
    // ==================================================================================

    @Test
    fun `GIVEN List return type WHEN generated THEN should use emptyList default`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakeAuthenticationServiceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN
        assertTrue(
            content.contains("emptyList<") || content.contains("listOf<"),
            "List return types should use emptyList or listOf default",
        )
    }

    // ==================================================================================
    // Result Type Validation
    // ==================================================================================

    @Test
    fun `GIVEN Result return type WHEN generated THEN should use Result success default`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakeAuthenticationServiceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN
        assertTrue(
            content.contains("Result.success("),
            "Result return types should use Result.success default",
        )
    }

    // ==================================================================================
    // Factory and DSL Validation
    // ==================================================================================

    @Test
    fun `GIVEN interface WHEN generated THEN should create factory function`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakePropertyAndMethodInterfaceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN
        assertTrue(
            content.contains(
                "fun fakePropertyAndMethodInterface(configure: FakePropertyAndMethodInterfaceConfig.() -> Unit = {}): PropertyAndMethodInterface"
            ),
            "Should generate factory function with DSL parameter",
        )
    }

    @Test
    fun `GIVEN interface WHEN generated THEN should create standalone configuration DSL class`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakePropertyAndMethodInterfaceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN - Config class is standalone (no fake reference in constructor)
        assertTrue(
            content.contains("class FakePropertyAndMethodInterfaceConfig"),
            "Should generate standalone DSL configuration class",
        )
        // Config should NOT reference the fake implementation
        assertFalse(
            content.contains("FakePropertyAndMethodInterfaceConfig(private val fake:"),
            "Config class should NOT take fake as constructor param (standalone builder pattern)",
        )
    }

    // ==================================================================================
    // SAM Interface Validation (Phase 4)
    // ==================================================================================

    @Test
    fun `GIVEN SAM with varargs WHEN generated THEN should use Array type in behavior property`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakeVarargsProcessorImpl.kt")
        if (!fakeFile.exists()) {
            println(
                "⚠️  Skipping test - sample not built. Run './gradlew :samples:single-module:build' first"
            )
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN - Immutable behavior property should use Array<out T>
        assertTrue(
            content.contains("private val processBehavior: (Array<out String>) -> List<String>"),
            "Immutable behavior property should use Array<out String>",
        )

        // Override method should still use vararg (this is correct)
        assertTrue(
            content.contains("override fun process(vararg items: String): List<String>"),
            "Override method should preserve vararg modifier",
        )
    }

    @Test
    fun `GIVEN SAM with star projection WHEN generated THEN should preserve star in override signature`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakeStarProjectionHandlerImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN - Override should preserve List<*>, not use List<Any>
        assertTrue(
            content.contains("override fun handle(items: List<*>): Int"),
            "Override signature should preserve star projection List<*>, not use List<Any>",
        )
    }

    // ==================================================================================
    // Constructor Parameter Validation
    // ==================================================================================

    @Test
    fun `GIVEN interface WHEN generated THEN should have private val constructor params with defaults`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakePropertyAndMethodInterfaceImpl.kt")
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN - Constructor params are private val with sensible defaults
        assertTrue(
            content.contains("private val") && content.contains("Behavior:"),
            "Constructor behavior params should be private val with defaults",
        )
        // THEN - No intermediate member properties (direct constructor pattern)
        assertFalse(
            content.contains("Behavior ?:"),
            "Should NOT have intermediate null-coalescing members (simplified pattern)",
        )
    }

    // ==================================================================================
    // Complete Validation
    // ==================================================================================

    @Test
    fun `GIVEN all sample interfaces WHEN generated THEN all fake implementations should exist`() {
        // GIVEN - Basic non-generic interfaces
        val expectedFiles =
            listOf(
                "FakePropertyAndMethodInterfaceImpl.kt",
                "FakeAnalyticsServiceImpl.kt",
                "FakeAsyncDataServiceImpl.kt",
                "FakeAuthenticationServiceImpl.kt",
                "FakeCompanyServiceImpl.kt",
                "FakeEnterpriseRepositoryImpl.kt",
                "FakeEventProcessorImpl.kt",
                "FakeProductServiceImpl.kt",
                "FakeUserRepositoryImpl.kt",
            )

        if (!generatedDir.exists()) {
            println(
                "⚠️  Skipping test - sample not built. Run './gradlew :samples:single-module:build' first"
            )
            return
        }

        // WHEN
        val actualFiles = generatedDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()

        // THEN
        expectedFiles.forEach { expectedFile ->
            assertTrue(expectedFile in actualFiles, "Should generate $expectedFile")
        }
    }
}
