// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import com.rsicarelli.fakt.Fake

/** Debug-only fake: must reach the debug unit tests and no other variant. */
@Fake
interface DebugMenu {
    fun items(): List<String>
}
