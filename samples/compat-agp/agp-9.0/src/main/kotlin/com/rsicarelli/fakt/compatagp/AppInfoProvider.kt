// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compatagp

import android.content.Context
import com.rsicarelli.fakt.Fake

/**
 * An `@Fake` whose signature uses an Android framework type.
 *
 * This cell uses AGP 9's built-in Kotlin. Its `faktGenerateAndroidjvmDebug` task reads this file
 * through AGP's variant API (issue #154) and resolves `Context` against the Android SDK boot
 * classpath (issue #158).
 */
@Fake
interface AppInfoProvider {
    fun appName(context: Context): String

    fun versionCode(): Int
}
