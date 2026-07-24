// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.AnalyticsService
import benchmark.domain.ComplexApiService
import benchmark.domain.ConfigProvider
import benchmark.domain.User
import benchmark.domain.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/** Bloc A — per-operation cost for Mockito. Mirrors [FaktBenchmark]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockitoBenchmark {
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
        val analytics = mock<AnalyticsService>()
        repeat(Bench.invocations) {
            analytics.track("evt")
            analytics.identify("user")
        }
        verify(analytics, times(Bench.invocations)).track("evt")
        verify(analytics, times(Bench.invocations)).identify("user")
        return (Bench.invocations * 2).toLong()
    }

    private fun relaxedUnstubbed(): Long {
        // Unlike MockK's relaxed mode, plain Mockito returns null for unstubbed object methods — that
        // IS its relaxed behavior, so we read it null-safely (still measures the unstubbed dispatch).
        val svc = mock<ComplexApiService>()
        var cs = 0L
        repeat(Bench.invocations) {
            cs += (svc.simpleMethod()?.length ?: 0).toLong()
            cs += (svc.readOnlyProperty?.length ?: 0).toLong()
            cs += svc.mutableProperty.toLong()
        }
        return cs
    }

    private fun instantiationChurn(): Long {
        var cs = 0L
        repeat(Bench.churn) {
            cs += System.identityHashCode(mock<UserRepository>()).toLong()
            cs += System.identityHashCode(mock<AnalyticsService>()).toLong()
            cs += System.identityHashCode(mock<ConfigProvider>()).toLong()
            cs += System.identityHashCode(mock<ComplexApiService>()).toLong()
        }
        return cs
    }

    private fun propertyHeavy(): Long {
        val config = mock<ConfigProvider>()
        whenever(config.baseUrl).thenReturn("https://api.example.com")
        whenever(config.timeout).thenReturn(30_000L)
        whenever(config.retryCount).thenReturn(3)
        whenever(config.featureFlags).thenReturn(mapOf("a" to true, "b" to false))
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
        val repo = mock<UserRepository>()
        whenever(repo.getUser(any())).thenReturn(User("1", "name"))
        whenever(repo.saveUser(any())).thenReturn(Result.success(Unit))
        whenever(repo.listUsers(any())).thenReturn(emptyList())
        var cs = 0L
        repeat(Bench.invocations) {
            cs += (repo.getUser("1")?.name?.length ?: 0).toLong()
            cs += if (repo.saveUser(User("1", "n")).isSuccess) 1L else 0L
            cs += repo.listUsers(0).size.toLong()
        }
        cs
    }
}
