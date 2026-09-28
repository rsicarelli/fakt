// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compatagp

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Consumes a fake generated from an interface that uses `android.content.Context`. On this
 * built-in Kotlin cell the fake is generated in-process (see [AppInfoProvider]).
 *
 * Uses JUnit4 (not `kotlin.test`), like [CompatTest]: AGP 9.0's built-in Kotlin doesn't wire
 * `kotlin-test` onto the unit-test classpath.
 */
class AppInfoProviderTest {

    @Test
    fun `GIVEN an Android-typed fake WHEN configuring behavior THEN it applies`() {
        val fake =
            fakeAppInfoProvider {
                appName { _ -> "Fakt" }
                versionCode { 42 }
            }

        assertEquals(42, fake.versionCode())
    }
}
