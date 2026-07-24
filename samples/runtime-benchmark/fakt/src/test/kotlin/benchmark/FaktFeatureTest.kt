// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.fakeAnalyticsService
import kotlin.test.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/**
 * Bloc C — JUnit5 `@ParameterizedTest` amplifier. Each case gets a FRESH fake, so a technology's
 * per-instance creation cost is paid once per input. The suite's wall-clock (read from the JUnit
 * XML `time` attribute by the summary script) is the relatable, startup-inclusive number. Every
 * competitor ships an identical feature test; only the fake creation differs.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktFeatureTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun `fresh fake per parameterized case`(case: Int) {
        val analytics = fakeAnalyticsService { track { } }
        analytics.track("evt-$case")
        assertEquals(1, analytics.trackCalls.value.size)
    }

    companion object {
        @JvmStatic
        fun cases(): List<Int> {
            val n = System.getProperty("fakt.benchmark.featureInputs")?.toIntOrNull() ?: 100
            return (0 until n).toList()
        }
    }
}
