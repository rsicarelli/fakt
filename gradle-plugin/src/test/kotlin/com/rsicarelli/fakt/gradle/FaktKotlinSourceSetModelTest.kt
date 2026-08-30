// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the full truth table of [hasReadableKotlinSourceSets], the pure predicate that keeps the
 * cache-correct path off projects whose Kotlin source sets it cannot read.
 *
 * AGP 9 ships built-in Kotlin support and rejects KGP's `org.jetbrains.kotlin.android`. There the
 * `KotlinCompilation`s handed to the subplugin expose `KotlinSourceSet`s whose `srcDirs` are empty
 * — AGP keeps sources in its own variant model — so a `FaktGenerateTask` producer would resolve
 * zero sources, run as NO-SOURCE and silently generate nothing. Such projects must stay on the
 * in-process plugin. Locked end-to-end by the `samples/compat-agp/agp-9.0` CI cell; this table is
 * the unit-level guard.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktKotlinSourceSetModelTest {

    @Test
    fun `GIVEN non-Android project WHEN no kotlin-android plugin THEN source sets are readable`() {
        assertTrue(
            hasReadableKotlinSourceSets(hasAndroidPlugin = false, hasKotlinAndroidPlugin = false),
            "A JVM or KMP project always populates the KGP source-set model.",
        )
    }

    @Test
    fun `GIVEN Android project WHEN kotlin-android plugin applied THEN source sets are readable`() {
        assertTrue(
            hasReadableKotlinSourceSets(hasAndroidPlugin = true, hasKotlinAndroidPlugin = true),
            "AGP 8.x + KGP populates srcDirs, so the cache-correct producer can read them.",
        )
    }

    @Test
    fun `GIVEN Android project WHEN kotlin-android plugin absent THEN source sets are NOT readable`() {
        assertFalse(
            hasReadableKotlinSourceSets(hasAndroidPlugin = true, hasKotlinAndroidPlugin = false),
            "AGP built-in Kotlin leaves srcDirs empty — the producer would generate nothing.",
        )
    }

    @Test
    fun `GIVEN non-Android project WHEN kotlin-android plugin somehow applied THEN source sets are readable`() {
        assertTrue(
            hasReadableKotlinSourceSets(hasAndroidPlugin = false, hasKotlinAndroidPlugin = true),
            "Only the Android case can lack the source-set model; everything else has one.",
        )
    }
}
