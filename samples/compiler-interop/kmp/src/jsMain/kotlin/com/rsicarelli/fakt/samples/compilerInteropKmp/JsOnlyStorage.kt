// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInteropKmp

import com.rsicarelli.fakt.Fake

/** JS-only fake: owned by the js producer, which needs the same options as commonMain. */
@Fake
interface JsOnlyStorage {
    fun read(key: String): String?
}

/** Uses the opt-in marker and an unqualified enum entry, like commonMain does. */
fun jsChannelLabel(channel: Channel, storage: JsOnlyStorage): String =
    when (channel) {
        EMAIL -> "${interopStamp()}:email:${storage.read("outbox")}"
        SMS -> "${interopStamp()}:sms:${storage.read("outbox")}"
    }
