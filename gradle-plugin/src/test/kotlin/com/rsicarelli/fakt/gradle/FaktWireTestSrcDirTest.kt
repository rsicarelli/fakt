// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the variant-aware, test-fixtures-aware selection contract of [shouldWireGeneratedDir], the
 * predicate that decides which `*Test` compile tasks source a non-KMP producer's generated fakes.
 *
 * Regression cover for issue #79 P8 blocker 1 (an Android target registers one producer per build
 * variant; feeding every producer into every `*Test` compile duplicated declarations → overload
 * resolution ambiguity) and gap 3 (`useGradleTestFixtures` must route fakes to `testFixtures` only,
 * not `test`). Both bugs lived in the same blunt `name.contains("test")` filter.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktWireTestSrcDirTest {

    @Test
    fun `GIVEN android debug producer with fixtures off WHEN matching the debug unit-test compile THEN it is wired`() {
        assertTrue(
            shouldWireGeneratedDir("compileDebugUnitTestKotlin", "debug", useTestFixtures = false),
            "The debug producer must feed the debug unit-test compilation.",
        )
    }

    @Test
    fun `GIVEN android debug producer with fixtures off WHEN matching the release unit-test compile THEN it is not wired`() {
        assertFalse(
            shouldWireGeneratedDir(
                "compileReleaseUnitTestKotlin",
                "debug",
                useTestFixtures = false,
            ),
            "The debug producer must NOT feed the release unit-test compilation — cross-variant " +
                "wiring duplicates the same fakes and breaks overload resolution (blocker 1).",
        )
    }

    @Test
    fun `GIVEN android release producer with fixtures off WHEN matching the release unit-test compile THEN it is wired`() {
        assertTrue(
            shouldWireGeneratedDir(
                "compileReleaseUnitTestKotlin",
                "release",
                useTestFixtures = false,
            ),
            "The release producer must feed the release unit-test compilation.",
        )
    }

    @Test
    fun `GIVEN android release producer with fixtures off WHEN matching the debug unit-test compile THEN it is not wired`() {
        assertFalse(
            shouldWireGeneratedDir(
                "compileDebugUnitTestKotlin",
                "release",
                useTestFixtures = false,
            ),
            "The release producer must NOT feed the debug unit-test compilation (blocker 1).",
        )
    }

    @Test
    fun `GIVEN android debug producer with fixtures off WHEN matching the debug instrumented-test compile THEN it is wired`() {
        assertTrue(
            shouldWireGeneratedDir(
                "compileDebugAndroidTestKotlin",
                "debug",
                useTestFixtures = false,
            ),
            "Instrumented androidTest compilations are variant-scoped like unit tests.",
        )
    }

    @Test
    fun `GIVEN jvm main producer with fixtures off WHEN matching the test compile THEN it is wired`() {
        assertTrue(
            shouldWireGeneratedDir("compileTestKotlin", "main", useTestFixtures = false),
            "A JVM `main` producer must keep feeding the plain test compilation (unchanged path).",
        )
    }

    @Test
    fun `GIVEN jvm main producer with fixtures off WHEN matching the production compile THEN it is not wired`() {
        assertFalse(
            shouldWireGeneratedDir("compileKotlin", "main", useTestFixtures = false),
            "Fakes are test-only — the production compile must never source them.",
        )
    }

    @Test
    fun `GIVEN jvm main producer with fixtures off WHEN matching the test-fixtures compile THEN it is not wired`() {
        assertFalse(
            shouldWireGeneratedDir("compileTestFixturesKotlin", "main", useTestFixtures = false),
            "With fixtures disabled the testFixtures compilation must not receive fakes (gap 3).",
        )
    }

    @Test
    fun `GIVEN jvm main producer with fixtures on WHEN matching the test-fixtures compile THEN it is wired`() {
        assertTrue(
            shouldWireGeneratedDir("compileTestFixturesKotlin", "main", useTestFixtures = true),
            "With fixtures enabled fakes belong to the testFixtures compilation (gap 3).",
        )
    }

    @Test
    fun `GIVEN jvm main producer with fixtures on WHEN matching the test compile THEN it is not wired`() {
        assertFalse(
            shouldWireGeneratedDir("compileTestKotlin", "main", useTestFixtures = true),
            "With fixtures enabled the plain test compile reuses fakes via the implicit " +
                "testFixtures dependency — it must not source them directly, or they compile " +
                "into both `test` and `testFixtures` outputs (gap 3).",
        )
    }
}
