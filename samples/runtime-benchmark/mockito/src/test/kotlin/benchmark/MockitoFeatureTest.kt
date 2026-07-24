// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.AnalyticsService
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

/** Bloc C — `@ParameterizedTest` amplifier for Mockito (fresh mock per case). Mirrors [FaktFeatureTest]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockitoFeatureTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun `fresh mock per parameterized case`(case: Int) {
        val analytics = mock<AnalyticsService>()
        analytics.track("evt-$case")
        verify(analytics, times(1)).track("evt-$case")
    }

    companion object {
        @JvmStatic
        fun cases(): List<Int> {
            val n = System.getProperty("fakt.benchmark.featureInputs")?.toIntOrNull() ?: 100
            return (0 until n).toList()
        }
    }
}
