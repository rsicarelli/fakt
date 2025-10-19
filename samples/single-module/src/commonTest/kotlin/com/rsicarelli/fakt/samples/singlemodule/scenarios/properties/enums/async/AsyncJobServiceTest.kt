// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.properties.enums.async

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for AsyncJobService fake with enums in suspend/async contexts.
 *
 * Validates that enums work correctly with suspend functions and
 * coroutine-based code generation.
 */
class AsyncJobServiceTest {
    @Test
    fun `GIVEN AsyncJobService fake WHEN configuring currentStatus THEN should return enum status`() =
        runTest {
            // Given
            val asyncJobService =
                fakeAsyncJobService {
                    currentStatus { JobStatus.RUNNING }
                }

            // When
            val status = asyncJobService.currentStatus

            // Then
            assertEquals(JobStatus.RUNNING, status)
        }

    @Test
    fun `GIVEN AsyncJobService fake WHEN configuring getStatus THEN should return enum from suspend function`() =
        runTest {
            // Given
            val asyncJobService =
                fakeAsyncJobService {
                    getStatus { jobId ->
                        delay(10) // Simulate async operation
                        when (jobId) {
                            "job1" -> JobStatus.COMPLETED
                            "job2" -> JobStatus.RUNNING
                            "job3" -> JobStatus.FAILED
                            else -> JobStatus.IDLE
                        }
                    }
                }

            // When
            val status1 = asyncJobService.getStatus("job1")
            val status2 = asyncJobService.getStatus("job2")
            val status3 = asyncJobService.getStatus("unknown")

            // Then
            assertEquals(JobStatus.COMPLETED, status1)
            assertEquals(JobStatus.RUNNING, status2)
            assertEquals(JobStatus.IDLE, status3)
        }

    @Test
    fun `GIVEN AsyncJobService fake WHEN configuring waitForStatus THEN should wait for target enum status`() =
        runTest {
            // Given
            var currentStatus = JobStatus.IDLE
            val asyncJobService =
                fakeAsyncJobService {
                    getStatus { currentStatus }
                    waitForStatus { jobId, targetStatus ->
                        var attempts = 0
                        while (currentStatus != targetStatus && attempts < 10) {
                            delay(10)
                            attempts++
                        }
                        currentStatus == targetStatus
                    }
                }

            // When
            val waitResult = asyncJobService.waitForStatus("job1", JobStatus.RUNNING)
            currentStatus = JobStatus.RUNNING
            val waitResult2 = asyncJobService.waitForStatus("job1", JobStatus.RUNNING)

            // Then
            assertFalse(waitResult) // Timeout
            assertTrue(waitResult2) // Success
        }

    @Test
    fun `GIVEN AsyncJobService fake WHEN configuring executeJob THEN should return Result with enum status`() =
        runTest {
            // Given
            val asyncJobService =
                fakeAsyncJobService {
                    executeJob { jobId ->
                        delay(10) // Simulate async work
                        when {
                            jobId.startsWith("success") -> Result.success(JobStatus.COMPLETED)
                            jobId.startsWith("fail") -> Result.success(JobStatus.FAILED)
                            else -> Result.failure(IllegalArgumentException("Unknown job"))
                        }
                    }
                }

            // When
            val successResult = asyncJobService.executeJob("success-job")
            val failResult = asyncJobService.executeJob("fail-job")
            val errorResult = asyncJobService.executeJob("unknown")

            // Then
            assertTrue(successResult.isSuccess)
            assertEquals(JobStatus.COMPLETED, successResult.getOrNull())

            assertTrue(failResult.isSuccess)
            assertEquals(JobStatus.FAILED, failResult.getOrNull())

            assertTrue(errorResult.isFailure)
        }

    @Test
    fun `GIVEN AsyncJobService fake WHEN configuring getJobsByStatus THEN should filter jobs by enum status`() =
        runTest {
            // Given
            val jobs =
                mapOf(
                    "job1" to JobStatus.COMPLETED,
                    "job2" to JobStatus.RUNNING,
                    "job3" to JobStatus.COMPLETED,
                    "job4" to JobStatus.FAILED,
                )
            val asyncJobService =
                fakeAsyncJobService {
                    getJobsByStatus { status ->
                        delay(10) // Simulate async query
                        jobs.filterValues { it == status }.keys.toList()
                    }
                }

            // When
            val completedJobs = asyncJobService.getJobsByStatus(JobStatus.COMPLETED)
            val runningJobs = asyncJobService.getJobsByStatus(JobStatus.RUNNING)
            val idleJobs = asyncJobService.getJobsByStatus(JobStatus.IDLE)

            // Then
            assertEquals(2, completedJobs.size)
            assertTrue(completedJobs.containsAll(listOf("job1", "job3")))

            assertEquals(1, runningJobs.size)
            assertEquals("job2", runningJobs.first())

            assertTrue(idleJobs.isEmpty())
        }

    @Test
    fun `GIVEN AsyncJobService fake WHEN configuring updateStatus THEN should update enum status asynchronously`() =
        runTest {
            // Given
            val statuses = mutableMapOf<String, JobStatus>()
            val asyncJobService =
                fakeAsyncJobService {
                    updateStatus { jobId, newStatus ->
                        delay(10) // Simulate async update
                        statuses[jobId] = newStatus
                    }
                    getStatus { jobId ->
                        statuses[jobId] ?: JobStatus.IDLE
                    }
                }

            // When
            asyncJobService.updateStatus("job1", JobStatus.RUNNING)
            asyncJobService.updateStatus("job1", JobStatus.COMPLETED)

            // Then
            assertEquals(JobStatus.COMPLETED, asyncJobService.getStatus("job1"))
        }

    @Test
    fun `GIVEN AsyncJobService fake WHEN executing multiple async operations THEN should handle concurrent enum operations`() =
        runTest {
            // Given
            val asyncJobService =
                fakeAsyncJobService {
                    executeJob { jobId ->
                        delay(10)
                        Result.success(JobStatus.COMPLETED)
                    }
                }

            // When - Execute multiple jobs concurrently
            val results =
                listOf("job1", "job2", "job3").map { jobId ->
                    asyncJobService.executeJob(jobId)
                }

            // Then
            assertEquals(3, results.size)
            assertTrue(results.all { it.isSuccess })
            assertTrue(results.all { it.getOrNull() == JobStatus.COMPLETED })
        }
}
