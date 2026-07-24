// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.User
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/** Bloc A — per-operation cost for hand-written fakes (the baseline). Mirrors [FaktBenchmark]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandwrittenBenchmark {
    @Test
    fun `run all mock-tax scenarios and emit timings`() {
        Bench.measure("verify-heavy") { verifyHeavy() }
        Bench.measure("relaxed-unstubbed") { relaxedUnstubbed() }
        Bench.measure("instantiation-churn") { instantiationChurn() }
        Bench.measure("property-heavy") { propertyHeavy() }
        Bench.measure("suspend-heavy") { suspendHeavy() }
        Bench.flush()
    }

    private fun verifyHeavy(): Long {
        val analytics = FakeAnalyticsService()
        repeat(Bench.invocations) {
            analytics.track("evt")
            analytics.identify("user")
        }
        return analytics.trackCount.toLong() + analytics.identifyCount.toLong()
    }

    private fun relaxedUnstubbed(): Long {
        val svc = FakeComplexApiService()
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
            cs += System.identityHashCode(FakeUserRepository()).toLong()
            cs += System.identityHashCode(FakeAnalyticsService()).toLong()
            cs += System.identityHashCode(FakeConfigProvider()).toLong()
            cs += System.identityHashCode(FakeComplexApiService()).toLong()
        }
        return cs
    }

    private fun propertyHeavy(): Long {
        val config = FakeConfigProvider(
            baseUrl = "https://api.example.com",
            timeout = 30_000L,
            retryCount = 3,
            featureFlags = mapOf("a" to true, "b" to false),
        )
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
        val repo = FakeUserRepository(
            onGet = { id -> User(id, "name") },
            onSave = { Result.success(Unit) },
            onList = { emptyList() },
        )
        var cs = 0L
        repeat(Bench.invocations) {
            cs += (repo.getUser("1")?.name?.length ?: 0).toLong()
            cs += if (repo.saveUser(User("1", "n")).isSuccess) 1L else 0L
            cs += repo.listUsers(0).size.toLong()
        }
        cs
    }
}
