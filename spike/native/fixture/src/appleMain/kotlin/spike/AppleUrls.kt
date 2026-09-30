// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import com.rsicarelli.fakt.Fake
import platform.Foundation.NSData
import platform.Foundation.NSURL

/** C2: shared appleMain over Foundation. */
@Fake
interface AppleUrls {
    fun resolve(path: String): NSURL?
    suspend fun fetch(url: NSURL): NSData?
}
