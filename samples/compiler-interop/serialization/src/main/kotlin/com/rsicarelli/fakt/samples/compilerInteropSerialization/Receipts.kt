// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInteropSerialization

import com.rsicarelli.fakt.Fake
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** `serializer()` is added by the serialization compiler plugin, which Fakt's worker lacks. */
@Serializable data class Receipt(val id: String, val amount: Int)

/** Main code (not a @Fake) that needs the plugin-generated `Receipt.serializer()`. */
fun encodeReceipt(receipt: Receipt): String = Json.encodeToString(Receipt.serializer(), receipt)

/** Signature uses plain strings only, so the worker never needs the serializable type. */
@Fake
interface ReceiptSink {
    fun write(json: String): Boolean
}

@Fake
interface Clock {
    fun now(): Long
}

class ReceiptWriter(private val sink: ReceiptSink, private val clock: Clock) {
    fun write(receipt: Receipt): Boolean =
        sink.write("${clock.now()}:${encodeReceipt(receipt)}")
}
