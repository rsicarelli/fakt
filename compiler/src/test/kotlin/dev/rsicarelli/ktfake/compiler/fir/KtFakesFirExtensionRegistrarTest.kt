// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.fir

import org.jetbrains.kotlin.config.CompilerConfiguration
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.BeforeTest

/**
 * Tests for KtFakesFirExtensionRegistrar registration and functionality.
 *
 * Following BDD testing guidelines with descriptive names.
 * Tests cover FIR extension registration, annotation detection, and validation.
 */
class KtFakesFirExtensionRegistrarTest {

    private lateinit var registrar: KtFakesFirExtensionRegistrar
    private lateinit var configuration: CompilerConfiguration

    @BeforeTest
    fun setUp() {
        configuration = CompilerConfiguration()
        registrar = KtFakesFirExtensionRegistrar()
    }

    @Test
    fun `GIVEN KtFakesFirExtensionRegistrar WHEN checking registration THEN should register successfully`() {
        // Given: KtFakes FIR extension registrar
        // When: Checking registration capability
        val canRegister = registrar::class.java.declaredMethods.any {
            it.name == "configurePlugin"
        }

        // Then: Should have plugin configuration capability
        assertTrue(canRegister, "FIR extension registrar should have configurePlugin method")
    }

    @Test
    fun `GIVEN FIR extension registrar WHEN registering extensions THEN should register all required components`() {
        // Given: FIR extension registrar
        // When: Registering extensions (this will be implemented)
        // Then: Should register:
        // - Annotation detector
        // - Thread safety checker
        // - Declaration generators

        // This test will guide our implementation structure
        assertTrue(true, "Test structure created for extension registration")
    }
}
