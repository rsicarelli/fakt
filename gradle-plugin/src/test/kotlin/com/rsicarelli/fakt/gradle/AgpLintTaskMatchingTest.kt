// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/** Pins which tasks [isAgpLintAnalysisTask] wires behind the generator. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgpLintTaskMatchingTest {

    @Test
    fun `GIVEN task names WHEN matching AGP lint analysis THEN only AGP lint tasks that read the sources match`() {
        val expected =
            mapOf(
                "lintAnalyzeDebug" to true,
                "lintAnalyzeDebugUnitTest" to true,
                "lintAnalyzeAndroidHostTest" to true,
                "lintVitalAnalyzeRelease" to true,
                "generateDebugLintModel" to true,
                "generateDebugUnitTestLintModel" to true,
                "generateDebugAndroidTestLintModel" to true,
                "generateReleaseLintReportModel" to true,
                "generateReleaseLintVitalReportModel" to true,
                "generateFreeDebugUnitTestLintModel" to true,
                "generateDebugResources" to false,
                "generateDebugLintKotlin" to false,
                "lintKotlin" to false,
                "lintKotlinMain" to false,
                "lintFix" to false,
                "lint" to false,
                "detekt" to false,
            )

        val actual = expected.keys.associateWith(::isAgpLintAnalysisTask)

        assertEquals(expected, actual)
    }
}
