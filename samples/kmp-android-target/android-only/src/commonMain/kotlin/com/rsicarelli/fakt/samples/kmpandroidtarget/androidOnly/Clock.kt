// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.androidOnly

import com.rsicarelli.fakt.Fake

/**
 * The only target is Android, so commonMain has no metadata compilation: the debug-preferred variant
 * emits this fake once, into commonTest.
 */
@Fake
interface Clock {
    fun now(): Long
}

expect fun deviceName(): String
