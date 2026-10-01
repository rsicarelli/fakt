// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.compilerInteropKmp

import com.rsicarelli.fakt.Fake

/**
 * ERROR-level opt-in marker, enabled for the module through `languageSettings.optIn` in the build
 * script, so no use site below carries an opt-in annotation.
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR, message = "Interop API: opt in module-wide.")
@Retention(AnnotationRetention.BINARY)
annotation class InteropMarker

@InteropMarker fun interopStamp(): String = "interop"

enum class Channel {
    EMAIL,
    SMS,
}

/** Unqualified enum entries: compiles only with `-Xcontext-sensitive-resolution`. */
fun priority(channel: Channel): Int =
    when (channel) {
        EMAIL -> 1
        SMS -> 2
    }

@Fake
interface Notifier {
    fun send(channel: Channel, message: String): Boolean
}

@Fake
interface DeliveryLog {
    suspend fun record(entry: String)
}

class Dispatcher(private val notifier: Notifier, private val log: DeliveryLog) {
    suspend fun dispatch(channel: Channel, message: String): Boolean {
        val delivered = notifier.send(channel, "${interopStamp()}:$message")
        log.record("${priority(channel)}:$message")
        return delivered
    }
}
