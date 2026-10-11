// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake

/**
 * Windows-only service. `mingwMain` is shared only by `mingwX64`, so only that target's consumer
 * can emit this fake, and the leaf `mingwX64Main` is empty (#152). The sample would stop
 * compiling the mingw tests if an empty leaf made its consumer task NO-SOURCE.
 */
@Fake
interface MingwOnlyService {
    /** Reads a registry value by [key]. */
    suspend fun readRegistryValue(key: String): String

    /** The Windows directory, e.g. `C:\Windows`. */
    fun windowsDirectory(): String
}
