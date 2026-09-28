// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.kmpandroidlint

import android.content.Context
import com.rsicarelli.fakt.Fake

/**
 * An `@Fake` on the KMP Android library target whose signature uses an Android framework type.
 * Fakt's generation task must analyse it against the Android SDK boot classpath, which KGP leaves
 * out of the compilation's dependencies (issue #158).
 */
@Fake
interface AppInfoProvider {
    fun appName(context: Context): String

    fun versionCode(): Int
}
