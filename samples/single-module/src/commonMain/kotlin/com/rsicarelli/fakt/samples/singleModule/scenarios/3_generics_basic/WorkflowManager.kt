// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.generics_basic

import com.rsicarelli.fakt.Fake

/**
 * P2.2: Method-level generics with lambda parameters.
 *
 * Tests method-level type parameters combined with lambda function parameters.
 * Common in workflow/pipeline patterns where steps can return different types (T)
 * and the framework needs to support async execution with suspend lambdas.
 */
@Fake
interface WorkflowManager {
    fun <T> executeStep(step: () -> T): T

    fun <T> executeStepWithFallback(
        step: () -> T,
        fallback: () -> T,
    ): T

    suspend fun <T> executeAsyncStep(step: suspend () -> T): T

    fun chainSteps(steps: List<() -> Unit>)
}
