// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import android.content.Context
import com.rsicarelli.fakt.Fake

/** Android-only fake: reaches each variant's unit and instrumented tests, never the JVM target. */
@Fake
interface AndroidOnly {
    val label: String

    fun describe(context: Context): String
}

actual fun platformName(): String = "android"
