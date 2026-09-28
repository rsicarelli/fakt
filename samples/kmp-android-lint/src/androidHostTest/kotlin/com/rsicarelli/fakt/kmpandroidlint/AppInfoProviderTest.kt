// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.kmpandroidlint

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Consumes a fake generated on the KMP Android library target from an interface that uses
 * `android.content.Context` (issue #158). Compiling this file proves the generated fake resolved
 * the Android type.
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
        assertEquals(1, fake.versionCodeCalls.value.size)
    }
}
