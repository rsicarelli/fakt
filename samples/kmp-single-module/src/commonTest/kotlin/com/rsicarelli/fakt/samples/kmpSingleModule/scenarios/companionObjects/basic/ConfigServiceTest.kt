// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.companionObjects.basic

import kotlin.test.Test
import kotlin.test.assertEquals

class ConfigServiceTest {
    @Test
    fun `GIVEN interface with companion property WHEN using fake THEN instance methods should work`() {
        // Given
        val service = fakeConfigService {
            getConfig { key -> "value-for-$key" }
        }

        // When
        val result = service.getConfig("api-key")

        // Then
        assertEquals("value-for-api-key", result)
    }

    @Test
    fun `GIVEN interface with companion property WHEN accessing companion THEN should return correct value`() {
        // Given - ConfigService companion property should exist

        // When
        val env = ConfigService.defaultEnvironment

        // Then
        assertEquals("production", env, "Companion property should be accessible")
    }
}
