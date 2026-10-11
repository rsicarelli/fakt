// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpMultiTarget

import com.rsicarelli.fakt.Fake
import platform.Foundation.NSData
import platform.Foundation.NSURL

/** Foundation types in appleMain (shared by ios and macos targets). */
@Fake
interface AppleUrls {
    fun resolve(path: String): NSURL?

    suspend fun fetch(url: NSURL?): NSData?
}
