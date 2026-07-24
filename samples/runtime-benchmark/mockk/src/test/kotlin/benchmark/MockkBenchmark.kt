// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.AnalyticsService
import benchmark.domain.ComplexApiService
import benchmark.domain.ConfigProvider
import benchmark.domain.User
import benchmark.domain.UserRepository
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/** Bloc A — per-operation cost for MockK (reflection + bytecode instrumentation). Mirrors [FaktBenchmark]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockkBenchmark {
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
        val analytics = mockk<AnalyticsService>()
        every { analytics.track(any()) } just Runs
        every { analytics.identify(any()) } just Runs
        repeat(Bench.invocations) {
            analytics.track("evt")
            analytics.identify("user")
        }
        verify(exactly = Bench.invocations) { analytics.track("evt") }
        verify(exactly = Bench.invocations) { analytics.identify("user") }
        return (Bench.invocations * 2).toLong()
    }

    private fun relaxedUnstubbed(): Long {
        val svc = mockk<ComplexApiService>(relaxed = true)
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
            cs += System.identityHashCode(mockk<UserRepository>(relaxed = true)).toLong()
            cs += System.identityHashCode(mockk<AnalyticsService>(relaxed = true)).toLong()
            cs += System.identityHashCode(mockk<ConfigProvider>(relaxed = true)).toLong()
            cs += System.identityHashCode(mockk<ComplexApiService>(relaxed = true)).toLong()
        }
        return cs
    }

    private fun propertyHeavy(): Long {
        val config = mockk<ConfigProvider>()
        every { config.baseUrl } returns "https://api.example.com"
        every { config.timeout } returns 30_000L
        every { config.retryCount } returns 3
        every { config.featureFlags } returns mapOf("a" to true, "b" to false)
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
        val repo = mockk<UserRepository>()
        coEvery { repo.getUser(any()) } returns User("1", "name")
        coEvery { repo.saveUser(any()) } returns Result.success(Unit)
        coEvery { repo.listUsers(any()) } returns emptyList()
        var cs = 0L
        repeat(Bench.invocations) {
            cs += (repo.getUser("1")?.name?.length ?: 0).toLong()
            cs += if (repo.saveUser(User("1", "n")).isSuccess) 1L else 0L
            cs += repo.listUsers(0).size.toLong()
        }
        cs
    }
}
