// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import com.rsicarelli.fakt.Fake

/** JVM-only fake: reaches jvmTest and nothing Android. */
@Fake
interface JvmOnly {
    fun pid(): Long
}

actual fun platformName(): String = "jvm"
