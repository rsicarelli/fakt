// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.properties.enums

import com.rsicarelli.fakt.Fake

/**
 * Task service with enum property.
 *
 * Tests:
 * - Enum properties generate with proper type-safe defaults
 * - Lambda type inference works with 'as Nothing' cast
 * - Property behavior can be configured via DSL
 *
 * This covers the bug fix: enum property defaults must use explicit cast
 * to avoid "expected Function0<Priority>, actual Function0<Unit>" error.
 */
@Fake
interface TaskService {
    /**
     * Default priority for new tasks.
     * Non-nullable enum property - tests 'as Nothing' cast fix.
     */
    val defaultPriority: Priority

    /**
     * Optional maximum priority allowed.
     * Nullable enum property - tests null default.
     */
    val maxPriority: Priority?

    /**
     * Create a task with specified priority.
     */
    fun createTask(
        name: String,
        priority: Priority,
    ): Task

    /**
     * Get tasks filtered by priority.
     */
    fun getTasksByPriority(priority: Priority): List<Task>
}

/**
 * Task data class for testing.
 */
data class Task(
    val name: String,
    val priority: Priority,
)
