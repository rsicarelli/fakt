// Copyright (C) 2026 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package spike

import com.rsicarelli.fakt.Fake

/** Common fake: owned by the commonMain producer, next to the Native producers. */
@Fake
interface Greeter {
    fun greet(name: String): String
}
