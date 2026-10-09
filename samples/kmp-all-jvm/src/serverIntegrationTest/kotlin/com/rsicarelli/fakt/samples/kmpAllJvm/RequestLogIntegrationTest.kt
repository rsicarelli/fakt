// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpAllJvm

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runs in the custom `serverIntegrationTest` compilation, associated with `server`'s `main` after
 * evaluation. The server fake reaches it through the association (#164b).
 */
class RequestLogIntegrationTest {

    @Test
    fun `GIVEN a custom test compilation associated with main WHEN using the server fake THEN it is generated`() {
        // Given
        val log = fakeRequestLog()

        // When
        log.record("/integration")

        // Then
        assertEquals(listOf("/integration"), log.recordCalls.value.map { it.path })
    }
}
