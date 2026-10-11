// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpNativeOnly.single

import com.rsicarelli.fakt.Fake
import platform.posix.stat

/** platform.posix types in the lone linuxX64Main: generated into linuxX64Test. */
@Fake
interface Stat {
    fun statistics(path: String): stat?

    fun exists(path: String): Boolean
}
