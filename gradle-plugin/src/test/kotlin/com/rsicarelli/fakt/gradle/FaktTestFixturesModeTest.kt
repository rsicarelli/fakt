// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the full truth table of [shouldEnableTestFixtures], the pure predicate behind
 * [FaktGradleSubplugin.resolveTestFixturesMode]. Test-fixtures mode requires the opt-in flag AND a
 * build that actually owns a `testFixtures` compilation — supplied by the `java-test-fixtures`
 * plugin (JVM) or by the Android Gradle plugin (`com.android.library` / `com.android.application`).
 * Extracting it as a `Project`-free function is what makes this table unit-testable without a
 * Gradle project.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktTestFixturesModeTest {

    @Test
    fun `GIVEN flag off WHEN neither fixtures source present THEN disabled`() {
        assertFalse(
            shouldEnableTestFixtures(
                useGradleTestFixtures = false,
                hasJavaTestFixtures = false,
                hasAndroidLibrary = false,
            ),
            "Without the opt-in flag fakes go to the default test source set.",
        )
    }

    @Test
    fun `GIVEN flag off WHEN java-test-fixtures present THEN disabled`() {
        assertFalse(
            shouldEnableTestFixtures(
                useGradleTestFixtures = false,
                hasJavaTestFixtures = true,
                hasAndroidLibrary = false,
            ),
            "The opt-in flag gates everything — an applied plugin alone must not enable it.",
        )
    }

    @Test
    fun `GIVEN flag on WHEN no fixtures source present THEN disabled`() {
        assertFalse(
            shouldEnableTestFixtures(
                useGradleTestFixtures = true,
                hasJavaTestFixtures = false,
                hasAndroidLibrary = false,
            ),
            "The flag alone is insufficient — there must be a testFixtures compilation to receive " +
                "the fakes, so the caller falls back to the default test source set.",
        )
    }

    @Test
    fun `GIVEN flag on WHEN java-test-fixtures present THEN enabled`() {
        assertTrue(
            shouldEnableTestFixtures(
                useGradleTestFixtures = true,
                hasJavaTestFixtures = true,
                hasAndroidLibrary = false,
            ),
            "The classic JVM path: flag + `java-test-fixtures` plugin.",
        )
    }

    @Test
    fun `GIVEN flag on WHEN android library present THEN enabled`() {
        assertTrue(
            shouldEnableTestFixtures(
                useGradleTestFixtures = true,
                hasJavaTestFixtures = false,
                hasAndroidLibrary = true,
            ),
            "The Android path: flag + AGP, where testFixtures comes from " +
                "`android { testFixtures { enable = true } }`.",
        )
    }

    @Test
    fun `GIVEN flag on WHEN both sources present THEN enabled`() {
        assertTrue(
            shouldEnableTestFixtures(
                useGradleTestFixtures = true,
                hasJavaTestFixtures = true,
                hasAndroidLibrary = true,
            ),
            "Either source is sufficient; having both still enables the mode.",
        )
    }
}
