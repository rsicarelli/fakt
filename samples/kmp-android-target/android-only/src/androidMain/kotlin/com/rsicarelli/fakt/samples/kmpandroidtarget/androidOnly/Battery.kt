// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.androidOnly

import com.rsicarelli.fakt.Fake

@Fake
interface Battery {
    fun level(): Int
}

actual fun deviceName(): String = "device"
