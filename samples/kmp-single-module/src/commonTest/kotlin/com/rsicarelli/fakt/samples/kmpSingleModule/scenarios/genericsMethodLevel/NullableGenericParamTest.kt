// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsMethodLevel

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Regression tests for #147: nullable method-level generic parameters in call verifiers. */
class NullableGenericParamTest {
    @Test
    fun `GIVEN nullable generic param WHEN called THEN verifier matches erased argument`() {
        // Given
        val flag = ToggleFlag("dark-mode")
        val store = fakeOverrideStore()

        // When
        store.setLocalOverride(flag, true)
        store.setLocalOverride<String>(flag)

        // Then
        store.verifySetLocalOverride {
            assertTrue(wasCalledWith(flag, true))
            assertTrue(wasCalledWith(flag, null))
            assertTrue(wasCalledTimes(2))
        }
    }

    @Test
    fun `GIVEN overloaded nullable generic param WHEN called THEN each verifier keeps its own signature`() {
        // Given
        val toggle = ToggleFlag("dark-mode")
        val experiment = ExperimentFlag("checkout")
        val received = mutableListOf<Any?>()
        val repo = fakeOverrideRepo {
            setLocalOverrideFeatureFlagAny { _: FeatureFlag, value: Int? -> received += value }
            setLocalOverrideExperimentFlagString { _, value -> received += value }
        }

        // When
        repo.setLocalOverride(toggle, 1)
        repo.setLocalOverride(experiment, "TREATMENT")

        // Then
        assertEquals(listOf<Any?>(1, "TREATMENT"), received)
        repo.verifySetLocalOverrideFeatureFlagAny { assertTrue(wasCalledWith(toggle, 1)) }
        repo.verifySetLocalOverrideExperimentFlagString {
            assertTrue(wasCalledWith(experiment, "TREATMENT"))
        }
    }
}
