// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compatagp

import android.content.Context
import com.rsicarelli.fakt.Fake

/**
 * An `@Fake` whose signature uses an Android framework type. The generation task must analyse it
 * against the Android SDK boot classpath, which KGP leaves out of the compilation's dependencies —
 * without it the build fails with "unresolved reference 'android'" (issue #158).
 */
@Fake
interface AppInfoProvider {
    fun appName(context: Context): String

    fun versionCode(): Int
}
