// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInteropSerialization

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReceiptWriterTest {

    @Test
    fun `GIVEN a receipt WHEN encoding THEN the plugin-generated serializer writes json`() {
        // Given
        val receipt = Receipt(id = "r-1", amount = 42)

        // When
        val json = encodeReceipt(receipt)

        // Then
        assertEquals("""{"id":"r-1","amount":42}""", json)
    }

    @Test
    fun `GIVEN fake sink and clock WHEN writing a receipt THEN the sink gets the stamped json`() {
        // Given
        val written = mutableListOf<String>()
        val writer =
            ReceiptWriter(
                sink = fakeReceiptSink { write { json -> written.add(json) } },
                clock = fakeClock { now { 7L } },
            )

        // When
        val ok = writer.write(Receipt(id = "r-2", amount = 5))

        // Then
        assertTrue(ok)
        assertEquals(listOf("""7:{"id":"r-2","amount":5}"""), written)
    }
}
