// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.codegen

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Validates generated code patterns from samples/single-module.
 *
 * **Testing Strategy**: Read actual generated files and validate expected patterns exist.
 * This pragmatic approach tests real compiler output without complex IR mocks.
 *
 * **Requirements**:
 * - samples/single-module must be built before running these tests
 * - Generated files must exist in build/generated/fakt/common/test/kotlin/
 *
 * **Coverage**:
 * - Behavior properties with correct type signatures
 * - Default value generation for all type categories
 * - Type preservation in generated code
 * - Configuration methods generation
 */
class GeneratedCodeValidationTest {
    private val generatedDir = File("samples/single-module/build/generated/fakt/common/test/kotlin/test/sample")

    // ==================================================================================
    // Behavior Properties Validation
    // ==================================================================================

    @Test
    fun `GIVEN simple interface WHEN generated THEN should create behavior properties with correct types`() {
        // GIVEN
        val fakeFile = File(generatedDir, "FakePropertyAndMethodInterfaceImpl.kt")

        // Skip if not built yet
        if (!fakeFile.exists()) {
            println("⚠️  Skipping test - sample not built. Run './gradlew :samples:single-module:build' first")
            return
        }

        // WHEN
        val content = fakeFile.readText()

        // THEN - Behavior properties should exist with exact types
        assertTrue(
            content.contains("private var getValueBehavior: () -> String"),
            "Should generate behavior property with correct String type",
        )
        assertTrue(
            content.contains("private var setValueBehavior: (String) -> Unit"),
            "Should generate behavior property with correct parameter type",
        )
        assertTrue(
            content.contains("private var stringValueBehavior: () -> String"),
            "Should generate behavior property for properties",
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
        assertTrue(
            content.contains("= { \"\" }"),
            "String default should be empty string",
        )
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
    // Configuration Methods Validation
    // ==================================================================================

    @Test
    fun `GIVEN interface methods WHEN generated THEN should create configure methods`() {
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
            content.contains("internal fun configureGetValue(behavior: () -> String)"),
            "Should generate configure method for functions",
        )
        assertTrue(
            content.contains("internal fun configureStringValue(behavior: () -> String)"),
            "Should generate configure method for properties",
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

        // THEN
        assertTrue(
            content.contains("private var fetchDataBehavior: suspend () -> String") ||
            content.contains("private var processDataBehavior: suspend"),
            "Should preserve suspend modifier in behavior property",
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
            content.contains("fun fakePropertyAndMethodInterface(configure: FakePropertyAndMethodInterfaceConfig.() -> Unit = {}): PropertyAndMethodInterface"),
            "Should generate factory function with DSL parameter",
        )
    }

    @Test
    fun `GIVEN interface WHEN generated THEN should create configuration DSL class`() {
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
            content.contains("class FakePropertyAndMethodInterfaceConfig(private val fake: FakePropertyAndMethodInterfaceImpl)"),
            "Should generate DSL configuration class",
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
            println("⚠️  Skipping test - sample not built. Run './gradlew :samples:single-module:build' first")
            return
        }

        // WHEN
        val actualFiles = generatedDir.listFiles()?.map { it.name }?.toSet() ?: emptySet()

        // THEN
        expectedFiles.forEach { expectedFile ->
            assertTrue(
                expectedFile in actualFiles,
                "Should generate $expectedFile",
            )
        }
    }
}
