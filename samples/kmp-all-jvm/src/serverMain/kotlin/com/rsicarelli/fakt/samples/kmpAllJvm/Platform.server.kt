// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpAllJvm

import com.rsicarelli.fakt.Fake

actual fun platformName(): String = "server"

/** Only the server target has a request log. Its fake goes to serverTest. */
@Fake
interface RequestLog {
    /** Records one handled request path. */
    fun record(path: String)
}
