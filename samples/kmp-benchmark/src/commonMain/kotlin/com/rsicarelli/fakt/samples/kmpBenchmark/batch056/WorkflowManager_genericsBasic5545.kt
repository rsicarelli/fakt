// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch056

import com.rsicarelli.fakt.Fake

@Fake
interface WorkflowManager_genericsBasic5545 {
    fun <T> executeStep(step: () -> T): T

    fun <T> executeStepWithFallback(
        step: () -> T,
        fallback: () -> T,
    ): T

    suspend fun <T> executeAsyncStep(step: suspend () -> T): T

    fun chainSteps(steps: List<() -> Unit>)
}
