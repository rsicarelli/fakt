// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpAllJvm

import com.rsicarelli.fakt.Fake

// desktopAndServerMain is the intermediate source set shared by the desktop and server targets
// (and not by cli). One actual covers both.

actual fun transport(): String = "gui-or-http"

@Fake
interface SessionStore {
    /** Opens a session for [user] and returns its token. */
    fun open(user: User): String

    /** Closes the session identified by [token]. */
    fun close(token: String)
}
