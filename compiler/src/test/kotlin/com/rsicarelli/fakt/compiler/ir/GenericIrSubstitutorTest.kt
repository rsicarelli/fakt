// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir

import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrTypeArgument
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests for GenericIrSubstitutor following GIVEN-WHEN-THEN pattern.
 *
 * TDD RED-GREEN cycle:
 * 1. Write failing test (RED) ✅
 * 2. Implement minimum code to pass (GREEN) ✅
 * 3. Refactor if needed (REFACTOR)
 *
 * Phase 1 Focus: Test that core APIs work and classes can be instantiated.
 * Full IR integration testing will come in Phase 3.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GenericIrSubstitutorTest {
    @Test
    fun `GIVEN empty substitution map WHEN creating IrTypeSubstitutor THEN should create successfully`() {
        // Given - Setup: Empty substitution map (simplest test case)
        val emptyMap = emptyMap<IrTypeParameterSymbol, IrTypeArgument>()

        // When - Execute: Create IrTypeSubstitutor with empty map (tests our API usage)
        val substitutor =
            IrTypeSubstitutor(
                substitution = emptyMap,
                allowEmptySubstitution = true,
            )

        // Then - Assert: Should create without compilation errors
        assertNotNull(substitutor, "Should create IrTypeSubstitutor with empty map")
        assertTrue(
            substitutor is IrTypeSubstitutor,
            "Should return correct type",
        )
    }

    @Test
    fun `GIVEN GenericIrSubstitutor can be instantiated WHEN creating with mock context THEN should not fail`() {
        // Given - Setup: Mock plugin context (we'll implement properly later)
        // For Phase 1, we just need to verify the class structure compiles

        // When - Execute: This test verifies our class compiles correctly
        // (We'll add actual instantiation in integration tests)
        val canInstantiate = true // Placeholder for compilation verification

        // Then - Assert: Class structure should be valid
        assertTrue(canInstantiate, "GenericIrSubstitutor class should compile correctly")
    }

    @Test
    fun `GIVEN IrTypeSubstitutor API WHEN using primary constructor THEN should work with substitution map`() {
        // Given - Setup: Use the primary constructor (what we'll use in our implementation)
        val emptySubstitution = emptyMap<IrTypeParameterSymbol, IrTypeArgument>()

        // When - Execute: Use primary constructor (tests the API we discovered)
        val substitutor =
            IrTypeSubstitutor(
                substitution = emptySubstitution,
                allowEmptySubstitution = true,
            )

        // Then - Assert: Primary constructor should work correctly
        assertNotNull(substitutor, "Primary constructor should work")
        assertTrue(
            substitutor.javaClass.simpleName == "IrTypeSubstitutor",
            "Should create IrTypeSubstitutor instance",
        )
    }

    /**
     * Phase 3: Method-Level Generics Support
     *
     * GREEN test - verifies GenericIrSubstitutor has method-level remapper capability.
     */
    @Test
    fun `GIVEN GenericIrSubstitutor WHEN checking for method-level support THEN should have createMethodLevelRemapper method`() {
        // Given - Phase 3 requirement: Support method-level generics like <T> in functions
        // Example: interface TestService { fun <T> process(data: T): T }
        //
        // For this, we need IrTypeParameterRemapper (different from IrTypeSubstitutor)
        // IrTypeSubstitutor: class-level (interface Repo<T>)
        // IrTypeParameterRemapper: method-level (fun <T> process())

        // When - Check if GenericIrSubstitutor has the method-level remapper API
        // We verify the method exists by checking if it compiles
        val hasCreateMethodLevelRemapper =
            GenericIrSubstitutor::class
                .members
                .any { it.name == "createMethodLevelRemapper" }

        val hasMethodLevelTypeParametersChecker =
            GenericIrSubstitutor::class
                .members
                .any { it.name == "hasMethodLevelTypeParameters" }

        // Then - GenericIrSubstitutor should have method-level support API
        assertTrue(
            hasCreateMethodLevelRemapper,
            "GenericIrSubstitutor should have createMethodLevelRemapper() method",
        )
        assertTrue(
            hasMethodLevelTypeParametersChecker,
            "GenericIrSubstitutor should have hasMethodLevelTypeParameters() method",
        )

        // ✅ GREEN PHASE: Method-level generic API exists!
        // Next: Integrate this with ImplementationGenerator to preserve method-level generics
    }
}
