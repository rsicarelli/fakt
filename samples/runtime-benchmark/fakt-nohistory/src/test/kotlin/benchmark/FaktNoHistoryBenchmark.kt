// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.User
import benchmark.domain.fakeAnalyticsService
import benchmark.domain.fakeComplexApiService
import benchmark.domain.fakeConfigProvider
import benchmark.domain.fakeUserRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Bloc A for Fakt with call-history DISABLED. Identical to [FaktBenchmark] except `verify-heavy`
 * cannot read `trackCalls` (no history is generated) — every scenario simply skips the StateFlow
 * recording, which is exactly the cost this column isolates.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktNoHistoryBenchmark {
    @Test
    fun `run all mock-tax scenarios and emit timings`() {
        Bench.measure("verify-heavy") { verifyHeavy() }
        Bench.measure("relaxed-unstubbed") { relaxedUnstubbed() }
        Bench.measure("instantiation-churn") { instantiationChurn() }
        Bench.measure("property-heavy") { propertyHeavy() }
        Bench.measure("suspend-heavy") { suspendHeavy() }
        Bench.flush()
    }

    /** No call history to verify — this is the point; measures the pure invocation cost. */
    private fun verifyHeavy(): Long {
        val analytics = fakeAnalyticsService {
            track { }
            identify { }
        }
        repeat(Bench.invocations) {
            analytics.track("evt")
            analytics.identify("user")
        }
        return Bench.invocations * 2L
    }

    private fun relaxedUnstubbed(): Long {
        val svc = fakeComplexApiService()
        var cs = 0L
        repeat(Bench.invocations) {
            cs += svc.simpleMethod().length.toLong()
            cs += svc.readOnlyProperty.length.toLong()
            cs += svc.mutableProperty.toLong()
        }
        return cs
    }

    private fun instantiationChurn(): Long {
        var cs = 0L
        repeat(Bench.churn) {
            cs += System.identityHashCode(fakeUserRepository { }).toLong()
            cs += System.identityHashCode(fakeAnalyticsService { }).toLong()
            cs += System.identityHashCode(fakeConfigProvider { }).toLong()
            cs += System.identityHashCode(fakeComplexApiService { }).toLong()
        }
        return cs
    }

    private fun propertyHeavy(): Long {
        val config = fakeConfigProvider {
            baseUrl { "https://api.example.com" }
            timeout { 30_000L }
            retryCount { 3 }
            featureFlags { mapOf("a" to true, "b" to false) }
        }
        var cs = 0L
        repeat(Bench.invocations) {
            cs += config.baseUrl.length.toLong()
            cs += config.timeout
            cs += (config.retryCount ?: 0).toLong()
            cs += config.featureFlags.size.toLong()
        }
        return cs
    }

    private fun suspendHeavy(): Long = runBlocking {
        val repo = fakeUserRepository {
            getUser { id -> User(id, "name") }
            saveUser { Result.success(Unit) }
            listUsers { emptyList() }
        }
        var cs = 0L
        repeat(Bench.invocations) {
            cs += (repo.getUser("1")?.name?.length ?: 0).toLong()
            cs += if (repo.saveUser(User("1", "n")).isSuccess) 1L else 0L
            cs += repo.listUsers(0).size.toLong()
        }
        cs
    }
}
