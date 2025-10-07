// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.`3_properties`.enums.async

import com.rsicarelli.fakt.Fake

/**
 * Async job service using enums with suspend functions.
 *
 * Tests:
 * - suspend functions returning enums
 * - suspend functions with enum parameters
 * - Result<Enum> with suspend
 * - Flow types with enums
 */
@Fake
interface AsyncJobService {
    /**
     * Current job status property.
     */
    val currentStatus: JobStatus

    /**
     * Get job status asynchronously.
     */
    suspend fun getStatus(jobId: String): JobStatus

    /**
     * Wait for job to reach specific status.
     */
    suspend fun waitForStatus(jobId: String, targetStatus: JobStatus): Boolean

    /**
     * Execute job and return final status.
     */
    suspend fun executeJob(jobId: String): Result<JobStatus>

    /**
     * Get all jobs with specific status.
     */
    suspend fun getJobsByStatus(status: JobStatus): List<String>

    /**
     * Update job status asynchronously.
     */
    suspend fun updateStatus(jobId: String, newStatus: JobStatus): Unit
}
