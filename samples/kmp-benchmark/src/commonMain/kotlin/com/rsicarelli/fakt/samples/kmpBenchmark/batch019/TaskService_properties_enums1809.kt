// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch019

import com.rsicarelli.fakt.samples.kmpBenchmark.models.Priority
import com.rsicarelli.fakt.Fake

@Fake
interface TaskService_properties_enums1809 {
    
    val defaultPriority: Priority

    
    val maxPriority: Priority?

    
    fun createTask(
        name: String,
        priority: Priority,
    ): TaskService_properties_enums1809_1

    
    fun getTasksByPriority(priority: Priority): List<TaskService_properties_enums1809_1>
}

data class TaskService_properties_enums1809_1(
    val name: String,
    val priority: Priority,
)
