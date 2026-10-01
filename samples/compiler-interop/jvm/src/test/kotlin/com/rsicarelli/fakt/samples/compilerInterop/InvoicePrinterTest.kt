// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInterop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvoicePrinterTest {

    @Test
    fun `GIVEN a pro invoice WHEN printing THEN renders the generated type and audits it`() {
        // Given
        val audit = fakeAuditLog()
        val renderer = fakeInvoiceRenderer { render { generated -> "invoice ${generated.origin}" } }
        val printer =
            InvoicePrinter(
                renderer = renderer,
                receipts = fakeReceiptStore { save { true } },
                audit = audit,
            )

        // When
        val text = printer.print(Invoice(id = "inv-7", tier = Tier.PRO), total = 100)

        // Then
        assertEquals("invoice inv-7", text)
        assertEquals(listOf("interop:inv-7"), audit.recordCalls.value.map { it.entry })
    }

    @Test
    fun `GIVEN a pro invoice WHEN printing THEN the discounted total reaches the receipt store`() {
        // Given
        val saved = mutableListOf<Receipt>()
        val printer =
            InvoicePrinter(
                renderer = fakeInvoiceRenderer { render { "ok" } },
                receipts = fakeReceiptStore { save { receipt -> saved.add(receipt) } },
                audit = fakeAuditLog(),
            )

        // When
        printer.print(Invoice(id = "inv-8", tier = Tier.PRO), total = 200)

        // Then
        assertEquals(listOf(Receipt(160)), saved)
    }

    @Test
    fun `GIVEN each tier WHEN asking for a discount THEN only paid tiers get one`() {
        // Given / When / Then
        assertEquals(0, discountPercent(Tier.FREE))
        assertEquals(20, discountPercent(Tier.PRO))
        assertTrue(isPaid(Tier.PRO))
    }
}
