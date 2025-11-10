// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.properties.enums

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskServiceTest {
    @Test
    fun `GIVEN TaskService fake WHEN configuring defaultPriority THEN should return configured priority`() {
        // Given
        val taskService = fakeTaskService {
            defaultPriority { Priority.HIGH }
        }

        // When
        val priority = taskService.defaultPriority

        // Then
        assertEquals(Priority.HIGH, priority)
    }

    @Test
    fun `GIVEN TaskService fake WHEN configuring maxPriority as null THEN should return null`() {
        // Given
        val taskService = fakeTaskService {
            maxPriority { null }
        }

        // When
        val priority = taskService.maxPriority

        // Then
        assertNull(priority)
    }

    @Test
    fun `GIVEN TaskService fake WHEN configuring maxPriority THEN should return configured priority`() {
        // Given
        val taskService = fakeTaskService {
            maxPriority { Priority.CRITICAL }
        }

        // When
        val priority = taskService.maxPriority

        // Then
        assertEquals(Priority.CRITICAL, priority)
    }

    @Test
    fun `GIVEN TaskService fake WHEN configuring createTask THEN should return configured task`() {
        // Given
        val expectedTask = Task("Write tests", Priority.HIGH)
        val taskService = fakeTaskService {
            createTask { name, priority ->
                Task(name, priority)
            }
        }

        // When
        val task = taskService.createTask("Write tests", Priority.HIGH)

        // Then
        assertEquals(expectedTask, task)
    }

    @Test
    fun `GIVEN TaskService fake WHEN configuring getTasksByPriority THEN should return filtered tasks`() {
        // Given
        val highPriorityTasks = listOf(
            Task("Fix bug", Priority.HIGH),
            Task("Release", Priority.HIGH),
        )
        val taskService = fakeTaskService {
            getTasksByPriority { priority ->
                when (priority) {
                    Priority.HIGH -> highPriorityTasks
                    else -> emptyList()
                }
            }
        }

        // When
        val tasks = taskService.getTasksByPriority(Priority.HIGH)

        // Then
        assertEquals(2, tasks.size)
        assertEquals(highPriorityTasks, tasks)
    }

    @Test
    fun `GIVEN TaskService fake with multiple priorities WHEN querying different priorities THEN should return correct tasks`() {
        // Given
        val lowPriorityTask = Task("Documentation", Priority.LOW)
        val criticalTask = Task("Security patch", Priority.CRITICAL)

        val taskService = fakeTaskService {
            getTasksByPriority { priority ->
                when (priority) {
                    Priority.LOW -> listOf(lowPriorityTask)
                    Priority.CRITICAL -> listOf(criticalTask)
                    else -> emptyList()
                }
            }
        }

        // When & Then
        assertEquals(listOf(lowPriorityTask), taskService.getTasksByPriority(Priority.LOW))
        assertEquals(listOf(criticalTask), taskService.getTasksByPriority(Priority.CRITICAL))
        assertEquals(emptyList(), taskService.getTasksByPriority(Priority.MEDIUM))
    }
}
