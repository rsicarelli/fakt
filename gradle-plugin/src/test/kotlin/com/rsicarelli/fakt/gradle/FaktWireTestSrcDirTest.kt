// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the variant-aware, test-fixtures-aware selection contract of [wiresTestCompile], the
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
            wiresTestCompile(
                "compileDebugUnitTestKotlin",
                "debug",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "The debug producer must feed the debug unit-test compilation.",
        )
    }

    @Test
    fun `GIVEN android debug producer with fixtures off WHEN matching the release unit-test compile THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileReleaseUnitTestKotlin",
                "debug",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "The debug producer must NOT feed the release unit-test compilation — cross-variant " +
                "wiring duplicates the same fakes and breaks overload resolution (blocker 1).",
        )
    }

    @Test
    fun `GIVEN android release producer with fixtures off WHEN matching the release unit-test compile THEN it is wired`() {
        assertTrue(
            wiresTestCompile(
                "compileReleaseUnitTestKotlin",
                "release",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "The release producer must feed the release unit-test compilation.",
        )
    }

    @Test
    fun `GIVEN android release producer with fixtures off WHEN matching the debug unit-test compile THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileDebugUnitTestKotlin",
                "release",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "The release producer must NOT feed the debug unit-test compilation (blocker 1).",
        )
    }

    @Test
    fun `GIVEN android debug producer with fixtures off WHEN matching the debug instrumented-test compile THEN it is wired`() {
        assertTrue(
            wiresTestCompile(
                "compileDebugAndroidTestKotlin",
                "debug",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "Instrumented androidTest compilations are variant-scoped like unit tests.",
        )
    }

    @Test
    fun `GIVEN jvm main producer with fixtures off WHEN matching the test compile THEN it is wired`() {
        assertTrue(
            wiresTestCompile(
                "compileTestKotlin",
                "main",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "A JVM `main` producer must keep feeding the plain test compilation (unchanged path).",
        )
    }

    @Test
    fun `GIVEN jvm main producer with fixtures off WHEN matching the production compile THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileKotlin",
                "main",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "Fakes are test-only — the production compile must never source them.",
        )
    }

    @Test
    fun `GIVEN jvm main producer with fixtures off WHEN matching the test-fixtures compile THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileTestFixturesKotlin",
                "main",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "With fixtures disabled the testFixtures compilation must not receive fakes (gap 3).",
        )
    }

    @Test
    fun `GIVEN jvm main producer with fixtures on WHEN matching the test-fixtures compile THEN it is wired`() {
        assertTrue(
            wiresTestCompile(
                "compileTestFixturesKotlin",
                "main",
                associatedWith = null,
                useTestFixtures = true,
            ),
            "With fixtures enabled fakes belong to the testFixtures compilation (gap 3).",
        )
    }

    @Test
    fun `GIVEN jvm main producer with fixtures on WHEN matching the test compile THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileTestKotlin",
                "main",
                associatedWith = null,
                useTestFixtures = true,
            ),
            "With fixtures enabled the plain test compile reuses fakes via the implicit " +
                "testFixtures dependency — it must not source them directly, or they compile " +
                "into both `test` and `testFixtures` outputs (gap 3).",
        )
    }

    @Test
    fun `GIVEN android main producer with fixtures on WHEN matching the debug test-fixtures compile THEN it is wired`() {
        assertTrue(
            wiresTestCompile(
                "compileDebugTestFixturesKotlin",
                "main",
                associatedWith = null,
                useTestFixtures = true,
            ),
            "The legacy path drives Android fixtures with a `main` producing token, so every " +
                "AGP variant's testFixtures compile (compileDebugTestFixturesKotlin) sources fakes.",
        )
    }

    @Test
    fun `GIVEN android main producer with fixtures on WHEN matching the release test-fixtures compile THEN it is wired`() {
        assertTrue(
            wiresTestCompile(
                "compileReleaseTestFixturesKotlin",
                "main",
                associatedWith = null,
                useTestFixtures = true,
            ),
            "Both AGP variants publish testFixtures; the release variant's compile must source " +
                "the same generated fakes.",
        )
    }

    @Test
    fun `GIVEN android main producer with fixtures on WHEN matching the debug unit-test compile THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileDebugUnitTestKotlin",
                "main",
                associatedWith = null,
                useTestFixtures = true,
            ),
            "With fixtures enabled the Android unit-test compile consumes fakes via the " +
                "testFixtures dependency — it must not source the generated dir directly.",
        )
    }

    @Test
    fun `GIVEN debug producer WHEN matching the debugMinified unit-test compile without association THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileDebugMinifiedUnitTestKotlin",
                "debug",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "`debug` is a substring of `debugMinified`; a variant match must be exact.",
        )
    }

    @Test
    fun `GIVEN debug producer WHEN matching the debugMinified unit-test compile associated with debugMinified THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileDebugMinifiedUnitTestKotlin",
                "debug",
                associatedWith = setOf("debugMinified"),
                useTestFixtures = false,
            ),
            "The association names the debugMinified compilation, not debug.",
        )
    }

    @Test
    fun `GIVEN release producer WHEN matching the preRelease unit-test compile THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compilePreReleaseUnitTestKotlin",
                "release",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "`release` is a substring of `preRelease`; a variant match must be exact.",
        )
    }

    @Test
    fun `GIVEN prodDebug producer WHEN matching the preprodDebug unit-test compile THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compilePreprodDebugUnitTestKotlin",
                "prodDebug",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "`proddebug` is a substring of `preproddebug`; a variant match must be exact.",
        )
    }

    @Test
    fun `GIVEN debugMinified producer WHEN matching its own unit-test compile THEN it is wired`() {
        assertTrue(
            wiresTestCompile(
                "compileDebugMinifiedUnitTestKotlin",
                "debugMinified",
                associatedWith = null,
                useTestFixtures = false,
            ),
            "A multi-word variant must still feed its own unit-test compilation.",
        )
    }

    @Test
    fun `GIVEN debug producer with fixtures on WHEN matching the debugMinified test-fixtures compile THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileDebugMinifiedTestFixturesKotlin",
                "debug",
                associatedWith = null,
                useTestFixtures = true,
            ),
            "Fixtures mode matches the variant exactly, or `debug` would redeclare the fakes.",
        )
    }

    @Test
    fun `GIVEN debug producer with fixtures on WHEN matching the debug test-fixtures compile THEN it is wired`() {
        assertTrue(
            wiresTestCompile(
                "compileDebugTestFixturesKotlin",
                "debug",
                associatedWith = null,
                useTestFixtures = true,
            ),
            "The debug producer feeds the debug testFixtures compilation.",
        )
    }

    @Test
    fun `GIVEN jvm main producer associated with main WHEN matching a custom integration compile THEN it is wired`() {
        assertTrue(
            wiresTestCompile(
                "compileIntegrationTestKotlin",
                "main",
                associatedWith = setOf("main"),
                useTestFixtures = false,
            ),
            "A test compilation associated with the producer's compilation receives its fakes.",
        )
    }

    @Test
    fun `GIVEN jvm main producer WHEN matching a test compile associated with another compilation THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileTestKotlin",
                "main",
                associatedWith = setOf("other"),
                useTestFixtures = false,
            ),
            "Association data wins over the name: this test compile belongs to another main.",
        )
    }

    @Test
    fun `GIVEN jvm main producer WHEN matching a custom compile associated with main THEN it is wired whatever its name`() {
        assertTrue(
            wiresTestCompile(
                "compileIntegrationKotlin",
                "main",
                associatedWith = setOf("main"),
                useTestFixtures = false,
            ),
            "A compilation associated with main is a test of main even without test in its name.",
        )
    }

    @Test
    fun `GIVEN jvm main producer associated with main WHEN matching the production compile THEN it is not wired`() {
        assertFalse(
            wiresTestCompile(
                "compileKotlin",
                "main",
                associatedWith = setOf("main"),
                useTestFixtures = false,
            ),
            "Fakes never reach a production compile, whatever the association says.",
        )
    }

    @Test
    fun `GIVEN debug producer associated with debug WHEN matching the debug unit-test compile THEN it is wired`() {
        assertTrue(
            wiresTestCompile(
                "compileDebugUnitTestKotlin",
                "debug",
                associatedWith = setOf("debug"),
                useTestFixtures = false,
            ),
            "The debug unit-test compilation is associated with debug.",
        )
    }
}
