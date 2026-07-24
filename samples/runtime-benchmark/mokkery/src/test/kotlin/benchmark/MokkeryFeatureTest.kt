// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.AnalyticsService
import dev.mokkery.MockMode
import dev.mokkery.mock
import dev.mokkery.verify
import dev.mokkery.verify.VerifyMode.Companion.exactly
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/** Bloc C — `@ParameterizedTest` amplifier for Mokkery (fresh mock per case). Mirrors [FaktFeatureTest]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MokkeryFeatureTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun `fresh mock per parameterized case`(case: Int) {
        val analytics = mock<AnalyticsService>(MockMode.autoUnit)
        analytics.track("evt-$case")
        verify(exactly(1)) { analytics.track("evt-$case") }
    }

    companion object {
        @JvmStatic
        fun cases(): List<Int> {
            val n = System.getProperty("fakt.benchmark.featureInputs")?.toIntOrNull() ?: 100
            return (0 until n).toList()
        }
    }
}
