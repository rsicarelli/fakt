// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.AnalyticsService
import benchmark.domain.ComplexApiService
import benchmark.domain.ConfigProvider
import benchmark.domain.User
import benchmark.domain.UserRepository
import io.mockative.any
import io.mockative.coEvery
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import io.mockative.verify
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Bloc A — per-operation cost for Mockative (KSP-generated mocks). Mirrors [FaktBenchmark].
 *
 * Note: Mockative has no "relaxed" mode, so the relaxed-unstubbed scenario explicitly stubs the
 * members it reads (documented in the README) — the closest honest equivalent.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockativeBenchmark {
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
        val analytics = mock(of<AnalyticsService>())
        every { analytics.track(any()) }.returns(Unit)
        every { analytics.identify(any()) }.returns(Unit)
        repeat(Bench.invocations) {
            analytics.track("evt")
            analytics.identify("user")
        }
        verify { analytics.track("evt") }.wasInvoked(exactly = Bench.invocations)
        verify { analytics.identify("user") }.wasInvoked(exactly = Bench.invocations)
        return (Bench.invocations * 2).toLong()
    }

    private fun relaxedUnstubbed(): Long {
        val svc = mock(of<ComplexApiService>())
        every { svc.simpleMethod() }.returns("")
        every { svc.readOnlyProperty }.returns("")
        every { svc.mutableProperty }.returns(0)
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
            cs += System.identityHashCode(mock(of<UserRepository>())).toLong()
            cs += System.identityHashCode(mock(of<AnalyticsService>())).toLong()
            cs += System.identityHashCode(mock(of<ConfigProvider>())).toLong()
            cs += System.identityHashCode(mock(of<ComplexApiService>())).toLong()
        }
        return cs
    }

    private fun propertyHeavy(): Long {
        val config = mock(of<ConfigProvider>())
        every { config.baseUrl }.returns("https://api.example.com")
        every { config.timeout }.returns(30_000L)
        every { config.retryCount }.returns(3)
        every { config.featureFlags }.returns(mapOf("a" to true, "b" to false))
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
        val repo = mock(of<UserRepository>())
        coEvery { repo.getUser(any()) }.returns(User("1", "name"))
        coEvery { repo.saveUser(any()) }.returns(Result.success(Unit))
        coEvery { repo.listUsers(any()) }.returns(emptyList())
        var cs = 0L
        repeat(Bench.invocations) {
            cs += (repo.getUser("1")?.name?.length ?: 0).toLong()
            cs += if (repo.saveUser(User("1", "n")).isSuccess) 1L else 0L
            cs += repo.listUsers(0).size.toLong()
        }
        cs
    }
}
