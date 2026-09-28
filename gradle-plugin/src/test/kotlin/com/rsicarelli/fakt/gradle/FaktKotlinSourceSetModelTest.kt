// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the full truth table of [hasReadableSources], the pure predicate that keeps the
 * cache-correct path off projects whose sources it cannot read.
 *
 * AGP 9 ships built-in Kotlin support and rejects KGP's `org.jetbrains.kotlin.android`. There the
 * `KotlinCompilation`s handed to the subplugin expose `KotlinSourceSet`s whose `srcDirs` are empty
 * — AGP keeps sources in its own variant model. The producer reads them from AGP's variant API
 * instead (issue #154), so such a module is only unreadable when that API is not visible to Fakt.
 * Locked end-to-end by the `samples/compat-agp/agp-9.0` CI cell; this table is the unit-level
 * guard.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktKotlinSourceSetModelTest {

    @Test
    fun `GIVEN a project not on AGP built-in Kotlin WHEN deciding THEN sources are readable`() {
        assertTrue(
            hasReadableSources(usesBuiltInKotlin = false, canReadVariantSources = false),
            "JVM, KMP and KGP-applied Android projects populate the KGP source-set model.",
        )
    }

    @Test
    fun `GIVEN AGP built-in Kotlin WHEN the variant API is readable THEN sources are readable`() {
        assertTrue(
            hasReadableSources(usesBuiltInKotlin = true, canReadVariantSources = true),
            "The producer reads the variant's source directories through androidComponents.",
        )
    }

    @Test
    fun `GIVEN AGP built-in Kotlin WHEN the variant API is not visible THEN sources are NOT readable`() {
        assertFalse(
            hasReadableSources(usesBuiltInKotlin = true, canReadVariantSources = false),
            "Empty srcDirs and no variant API: the producer would generate nothing.",
        )
    }
}
