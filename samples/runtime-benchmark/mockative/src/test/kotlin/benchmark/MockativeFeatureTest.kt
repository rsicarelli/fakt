// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.AnalyticsService
import io.mockative.any
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import io.mockative.verify
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/** Bloc C — `@ParameterizedTest` amplifier for Mockative (fresh mock per case). Mirrors [FaktFeatureTest]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockativeFeatureTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun `fresh mock per parameterized case`(case: Int) {
        val analytics = mock(of<AnalyticsService>())
        every { analytics.track(any()) }.returns(Unit)
        analytics.track("evt-$case")
        verify { analytics.track("evt-$case") }.wasInvoked(exactly = 1)
    }

    companion object {
        @JvmStatic
        fun cases(): List<Int> {
            val n = System.getProperty("fakt.benchmark.featureInputs")?.toIntOrNull() ?: 100
            return (0 until n).toList()
        }
    }
}
