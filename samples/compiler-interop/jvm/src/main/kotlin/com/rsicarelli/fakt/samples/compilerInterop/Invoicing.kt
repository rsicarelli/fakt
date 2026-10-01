// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInterop

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.compilerInterop.processor.Generate

/** A source class: the KSP processor writes `GeneratedInvoice` for it. */
@Generate class Invoice(val id: String, val tier: Tier)

/** Signature uses the KSP-generated type, so the worker needs it on its source path. */
@Fake
interface InvoiceRenderer {
    fun render(invoice: GeneratedInvoice): String
}

/** Signature uses a marker-gated type, so the worker needs the module-wide opt-in. */
@Fake
interface ReceiptStore {
    fun save(receipt: Receipt): Boolean
}

@Fake
interface AuditLog {
    fun record(entry: String)
}

class InvoicePrinter(
    private val renderer: InvoiceRenderer,
    private val receipts: ReceiptStore,
    private val audit: AuditLog,
) {
    fun print(invoice: Invoice, total: Int): String {
        val discounted = total - total * discountPercent(invoice.tier) / 100
        receipts.save(Receipt(discounted))
        audit.record("${interopStamp()}:${invoice.id}")
        return renderer.render(GeneratedInvoice(origin = invoice.id))
    }
}
