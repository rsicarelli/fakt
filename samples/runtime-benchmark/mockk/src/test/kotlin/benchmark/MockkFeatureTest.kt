// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.AnalyticsService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/** Bloc C — `@ParameterizedTest` amplifier for MockK (fresh mock per case). Mirrors [FaktFeatureTest]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockkFeatureTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun `fresh mock per parameterized case`(case: Int) {
        val analytics = mockk<AnalyticsService>()
        every { analytics.track(any()) } just Runs
        analytics.track("evt-$case")
        verify { analytics.track("evt-$case") }
    }

    companion object {
        @JvmStatic
        fun cases(): List<Int> {
            val n = System.getProperty("fakt.benchmark.featureInputs")?.toIntOrNull() ?: 100
            return (0 until n).toList()
        }
    }
}
