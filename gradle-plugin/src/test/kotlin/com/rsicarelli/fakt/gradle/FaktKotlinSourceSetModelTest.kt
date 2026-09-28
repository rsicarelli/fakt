// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the full truth table of [unreadableSourcesReason], the pure decision that keeps the
 * cache-correct path off projects whose sources it cannot read.
 *
 * AGP 9 ships built-in Kotlin support and rejects KGP's `org.jetbrains.kotlin.android`. There the
 * `KotlinCompilation`s handed to the subplugin expose `KotlinSourceSet`s whose `srcDirs` are empty
 * — AGP keeps sources in its own variant model. The producer reads them from AGP's variant API
 * instead (issue #154), so such a module is only unreadable when Fakt could not hook that API.
 *
 * A multiplatform `androidTarget()` on `com.android.library` also lacks
 * `org.jetbrains.kotlin.android`, but it is not built-in Kotlin: KGP compiles one compilation per
 * variant, whose default source sets exclude `androidMain`. It must stay in-process until it has a
 * per-variant design, or its `androidMain` fakes would be dropped.
 *
 * Locked end-to-end by the `samples/compat-agp/agp-9.0` CI cell; this table is the unit-level
 * guard.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktKotlinSourceSetModelTest {

    @Test
    fun `GIVEN a project without a variant-model Android plugin WHEN deciding THEN sources are readable`() {
        assertNull(
            unreadableSourcesReason(
                hasAndroidPlugin = false,
                hasKotlinAndroidPlugin = false,
                isMultiplatform = true,
                canReadVariantSources = false,
            ),
            "JVM and KMP projects (including the KMP Android library plugin) have Kotlin source sets.",
        )
    }

    @Test
    fun `GIVEN an Android module compiled by the Kotlin Android plugin WHEN deciding THEN sources are readable`() {
        assertNull(
            unreadableSourcesReason(
                hasAndroidPlugin = true,
                hasKotlinAndroidPlugin = true,
                isMultiplatform = false,
                canReadVariantSources = false,
            ),
            "AGP 8.x, or AGP 9 with built-in Kotlin off, populates the KGP source sets.",
        )
    }

    @Test
    fun `GIVEN AGP built-in Kotlin WHEN the variant API is hooked THEN sources are readable`() {
        assertNull(
            unreadableSourcesReason(
                hasAndroidPlugin = true,
                hasKotlinAndroidPlugin = false,
                isMultiplatform = false,
                canReadVariantSources = true,
            ),
            "The producer reads the variant's source directories through androidComponents.",
        )
    }

    @Test
    fun `GIVEN AGP built-in Kotlin WHEN the variant API could not be hooked THEN explains how to fix it`() {
        val reason =
            unreadableSourcesReason(
                hasAndroidPlugin = true,
                hasKotlinAndroidPlugin = false,
                isMultiplatform = false,
                canReadVariantSources = false,
            )

        assertNotNull(
            reason,
            "Empty srcDirs and no variant API: the producer would generate nothing.",
        )
        assertTrue("same build classpath as Fakt" in reason, reason)
    }

    @Test
    fun `GIVEN a multiplatform androidTarget on com android library WHEN deciding THEN stays in-process even with the variant API`() {
        val reason =
            unreadableSourcesReason(
                hasAndroidPlugin = true,
                hasKotlinAndroidPlugin = false,
                isMultiplatform = true,
                canReadVariantSources = true,
            )

        assertNotNull(reason, "Per-variant compilations would drop androidMain fakes.")
        assertTrue("androidTarget()" in reason, reason)
    }
}
