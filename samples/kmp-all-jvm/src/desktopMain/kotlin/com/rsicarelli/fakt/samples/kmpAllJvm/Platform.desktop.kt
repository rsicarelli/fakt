// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpAllJvm

import com.rsicarelli.fakt.Fake

actual fun platformName(): String = "desktop"

/** Only the desktop target has windows. Its fake goes to desktopTest. */
@Fake
interface WindowManager {
    /** Opens a window and returns its id. */
    fun open(title: String): Int
}
