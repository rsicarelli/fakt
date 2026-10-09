// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the pure compile-task-name parsing of [testCompileVariant] and [isTestFixturesCompileTask].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestWiringByAssociationTest {

    @Test
    fun `GIVEN a multi-word variant unit-test task WHEN parsing THEN variant and kind are split`() {
        assertEquals(
            TestCompileTask("debugMinified", TestCompileKind.UNIT),
            testCompileVariant("compileDebugMinifiedUnitTestKotlin"),
        )
    }

    @Test
    fun `GIVEN a preRelease unit-test task WHEN parsing THEN the variant keeps its inner capital`() {
        assertEquals(
            TestCompileTask("preRelease", TestCompileKind.UNIT),
            testCompileVariant("compilePreReleaseUnitTestKotlin"),
        )
    }

    @Test
    fun `GIVEN an android instrumented task WHEN parsing THEN the kind is android test`() {
        assertEquals(
            TestCompileTask("debug", TestCompileKind.ANDROID_TEST),
            testCompileVariant("compileDebugAndroidTestKotlin"),
        )
    }

    @Test
    fun `GIVEN a variant test-fixtures task WHEN parsing THEN the kind is fixtures`() {
        assertEquals(
            TestCompileTask("debug", TestCompileKind.FIXTURES),
            testCompileVariant("compileDebugTestFixturesKotlin"),
        )
    }

    @Test
    fun `GIVEN the jvm test task WHEN parsing THEN the variant is main and the kind is test`() {
        assertEquals(
            TestCompileTask("main", TestCompileKind.TEST),
            testCompileVariant("compileTestKotlin"),
        )
    }

    @Test
    fun `GIVEN the jvm test-fixtures task WHEN parsing THEN the variant is main and the kind is fixtures`() {
        assertEquals(
            TestCompileTask("main", TestCompileKind.FIXTURES),
            testCompileVariant("compileTestFixturesKotlin"),
        )
    }

    @Test
    fun `GIVEN the production compile task WHEN parsing THEN it is not a test compile`() {
        assertNull(testCompileVariant("compileKotlin"))
    }

    @Test
    fun `GIVEN a variant production compile task WHEN parsing THEN it is not a test compile`() {
        assertNull(testCompileVariant("compileDebugKotlin"))
    }

    @Test
    fun `GIVEN test-fixtures compile task names WHEN classifying THEN they are recognised`() {
        assertTrue(isTestFixturesCompileTask("compileTestFixturesKotlin"))
        assertTrue(isTestFixturesCompileTask("compileDebugTestFixturesKotlin"))
        assertTrue(isTestFixturesCompileTask("compileDebugMinifiedTestFixturesKotlin"))
    }

    @Test
    fun `GIVEN non-fixtures compile task names WHEN classifying THEN they are not recognised`() {
        assertFalse(isTestFixturesCompileTask("compileTestKotlin"))
        assertFalse(isTestFixturesCompileTask("compileDebugUnitTestKotlin"))
        assertFalse(isTestFixturesCompileTask("compileKotlin"))
    }
}
