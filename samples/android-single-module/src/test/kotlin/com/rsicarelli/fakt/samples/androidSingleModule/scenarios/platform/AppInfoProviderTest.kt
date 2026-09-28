// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.androidSingleModule.scenarios.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AppInfoProviderTest {
    @Test
    fun `GIVEN AppInfoProvider fake WHEN configuring behavior THEN it applies`() {
        // Given
        val fake =
            fakeAppInfoProvider {
                appName { _ -> "Fakt" }
                versionCode { 42 }
            }

        // When
        val versionCode = fake.versionCode()

        // Then
        assertEquals(42, versionCode)
        assertEquals(1, fake.versionCodeCalls.value.size)
    }

    @Test
    fun `GIVEN AppInfoProvider fake WHEN using defaults THEN nullable Android return is null`() {
        // Given
        val fake = fakeAppInfoProvider()

        // When
        val link = fake.deepLink("home")

        // Then
        assertNull(link)
        assertEquals(listOf("home"), fake.deepLinkCalls.value.map { it.path })
    }
}
