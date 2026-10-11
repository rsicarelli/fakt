// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import androidx.core.util.Consumer
import com.rsicarelli.fakt.Fake

/**
 * Android-only fake whose signature uses a type from an AAR dependency (`androidx.core:core-ktx`
 * brings `androidx.core.util.Consumer` in `core`, an `.aar`). The generation worker must read the
 * unpacked class jar of that AAR, or the type is unresolved.
 */
@Fake
interface AarTypeSource {
    fun subscribe(consumer: Consumer<String>)
}
