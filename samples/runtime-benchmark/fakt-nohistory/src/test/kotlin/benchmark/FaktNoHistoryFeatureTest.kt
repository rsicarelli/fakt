// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.fakeAnalyticsService
import kotlin.test.assertNotNull
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Bloc C for Fakt with call-history disabled — fresh fake per case. No `trackCalls` exists to
 * verify, so the case just exercises creation + invocation (which is the amplified cost anyway).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktNoHistoryFeatureTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun `fresh fake per parameterized case`(case: Int) {
        val analytics = fakeAnalyticsService { track { } }
        analytics.track("evt-$case")
        assertNotNull(analytics)
    }

    companion object {
        @JvmStatic
        fun cases(): List<Int> {
            val n = System.getProperty("fakt.benchmark.featureInputs")?.toIntOrNull() ?: 100
            return (0 until n).toList()
        }
    }
}
