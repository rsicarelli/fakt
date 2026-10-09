// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import com.rsicarelli.fakt.Fake

/**
 * Declared once in androidMain, but [BuildFlags] only exists in androidDebug and androidRelease
 * (same FQN, different members), so each variant must analyse its own copy.
 */
@Fake
interface FlagReader {
    fun read(): BuildFlags
}
