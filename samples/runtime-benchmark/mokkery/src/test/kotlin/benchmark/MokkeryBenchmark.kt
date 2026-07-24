// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.AnalyticsService
import benchmark.domain.ComplexApiService
import benchmark.domain.ConfigProvider
import benchmark.domain.User
import benchmark.domain.UserRepository
import dev.mokkery.MockMode
import dev.mokkery.answering.returns
import dev.mokkery.every
import dev.mokkery.everySuspend
import dev.mokkery.matcher.any
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/** Bloc A — per-operation cost for Mokkery (compile-time IR mocks). Mirrors [FaktBenchmark]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MokkeryBenchmark {
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
        val analytics = mock<AnalyticsService>(MockMode.autoUnit)
        repeat(Bench.invocations) {
            analytics.track("evt")
            analytics.identify("user")
        }
        verify(exactly(Bench.invocations)) { analytics.track("evt") }
        verify(exactly(Bench.invocations)) { analytics.identify("user") }
        return (Bench.invocations * 2).toLong()
    }

    private fun relaxedUnstubbed(): Long {
        val svc = mock<ComplexApiService>(MockMode.autofill)
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
            cs += System.identityHashCode(mock<UserRepository>(MockMode.autofill)).toLong()
            cs += System.identityHashCode(mock<AnalyticsService>(MockMode.autofill)).toLong()
            cs += System.identityHashCode(mock<ConfigProvider>(MockMode.autofill)).toLong()
            cs += System.identityHashCode(mock<ComplexApiService>(MockMode.autofill)).toLong()
        }
        return cs
    }

    private fun propertyHeavy(): Long {
        val config = mock<ConfigProvider>()
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
        val repo = mock<UserRepository>()
        everySuspend { repo.getUser(any()) } returns User("1", "name")
        everySuspend { repo.saveUser(any()) } returns Result.success(Unit)
        everySuspend { repo.listUsers(any()) } returns emptyList()
        var cs = 0L
        repeat(Bench.invocations) {
            cs += (repo.getUser("1")?.name?.length ?: 0).toLong()
            cs += if (repo.saveUser(User("1", "n")).isSuccess) 1L else 0L
            cs += repo.listUsers(0).size.toLong()
        }
        cs
    }
}
