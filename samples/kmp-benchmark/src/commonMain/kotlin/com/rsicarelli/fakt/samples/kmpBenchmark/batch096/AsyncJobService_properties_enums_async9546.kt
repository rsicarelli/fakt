// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch096

import com.rsicarelli.fakt.samples.kmpBenchmark.models.JobStatus
import com.rsicarelli.fakt.Fake

@Fake
interface AsyncJobService_properties_enums_async9546 {
    
    val currentStatus: JobStatus

    
    suspend fun getStatus(jobId: String): JobStatus

    
    suspend fun waitForStatus(
        jobId: String,
        targetStatus: JobStatus,
    ): Boolean

    
    suspend fun executeJob(jobId: String): Result<JobStatus>

    
    suspend fun getJobsByStatus(status: JobStatus): List<String>

    
    suspend fun updateStatus(
        jobId: String,
        newStatus: JobStatus,
    ): Unit
}
