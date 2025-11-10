// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.genericsBasic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class WorkflowManagerTest {
    @Test
    fun `GIVEN WorkflowManager with method-level generics WHEN generating fake THEN should preserve type parameters`() {
        // Given
        val workflow = fakeWorkflowManager()

        // When
        val stringResult: String = workflow.executeStep { "test" }
        val intResult: Int = workflow.executeStep { 42 }

        // Then
        assertEquals("test", stringResult)
        assertEquals(42, intResult)
    }

    @Test
    fun `GIVEN method-level generic with fallback WHEN configuring behavior THEN should maintain type safety`() {
        // Given - Method with generic and fallback parameter
        val workflow = fakeWorkflowManager {
            executeStepWithFallback<String> { step, fallback ->
                try {
                    step()
                } catch (e: Exception) {
                    fallback()
                }
            }
        }

        // When - Use with different types
        val result1: String = workflow.executeStepWithFallback(
            step = { "success" },
            fallback = { "fallback" },
        )

        // Then - Type safety preserved
        assertEquals("success", result1)
    }

    @Test
    fun `GIVEN non-generic method WHEN mixed with method-level generics THEN should work normally`() {
        // Given - Non-generic method chainSteps alongside generic methods
        var executedSteps = 0
        val workflow = fakeWorkflowManager {
            chainSteps { steps ->
                steps.forEach { step ->
                    step()
                    executedSteps++
                }
            }
        }

        // When - Execute steps
        workflow.chainSteps(
            listOf(
                { println("Step 1") },
                { println("Step 2") },
            ),
        )

        // Then - All steps executed
        assertEquals(2, executedSteps)
    }

    @Test
    fun `GIVEN WorkflowManager WHEN generated THEN fake should exist`() {
        // Given - WorkflowManager with method-level generics
        val workflow = fakeWorkflowManager()

        // Then - Should be successfully generated
        assertNotNull(workflow, "Fake should be generated for WorkflowManager")
    }
}
