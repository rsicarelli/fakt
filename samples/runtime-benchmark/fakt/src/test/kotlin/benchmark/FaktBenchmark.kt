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
 * Bloc A — per-operation cost for Fakt's compile-time generated fakes.
 *
 * The five scenarios each target a documented "mock tax" hot-spot (see the module README). Every
 * competitor implements this exact same set through the shared [Bench] harness; only the
 * create/configure/verify calls differ, mirroring docs/user-guide/migration-from-mocks.md.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktBenchmark {
    @Test
    fun `run all mock-tax scenarios and emit timings`() {
        Bench.measure("verify-heavy") { verifyHeavy() }
        Bench.measure("relaxed-unstubbed") { relaxedUnstubbed() }
        Bench.measure("instantiation-churn") { instantiationChurn() }
        Bench.measure("property-heavy") { propertyHeavy() }
        Bench.measure("suspend-heavy") { suspendHeavy() }
        Bench.flush()
    }

    /** Interaction verification — the 47x `verify` hot-spot. */
    private fun verifyHeavy(): Long {
        val analytics = fakeAnalyticsService {
            track { }
            identify { }
        }
        repeat(Bench.invocations) {
            analytics.track("evt")
            analytics.identify("user")
        }
        return analytics.trackCalls.value.size.toLong() + analytics.identifyCalls.value.size.toLong()
    }

    /** Unstubbed/relaxed calls returning smart defaults — the 3.7x `relaxed` hot-spot. */
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

    /** Creating many fresh fakes — final-class instrumentation churn (the 2.7-3x `mock-maker-inline`). */
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

    /** Property stubbing + reads. */
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

    /** Suspend stubbing + invocation — mocks pay extra coroutine wrapping. */
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
