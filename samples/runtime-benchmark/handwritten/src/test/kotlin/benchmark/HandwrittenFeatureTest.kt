// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import kotlin.test.assertEquals
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

/** Bloc C — `@ParameterizedTest` amplifier for hand-written fakes. Mirrors [FaktFeatureTest]. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HandwrittenFeatureTest {
    @ParameterizedTest
    @MethodSource("cases")
    fun `fresh fake per parameterized case`(case: Int) {
        val analytics = FakeAnalyticsService()
        analytics.track("evt-$case")
        assertEquals(1, analytics.trackCount)
    }

    companion object {
        @JvmStatic
        fun cases(): List<Int> {
            val n = System.getProperty("fakt.benchmark.featureInputs")?.toIntOrNull() ?: 100
            return (0 until n).toList()
        }
    }
}
