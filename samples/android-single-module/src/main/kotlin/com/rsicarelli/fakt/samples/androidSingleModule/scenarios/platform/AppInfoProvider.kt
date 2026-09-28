// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.androidSingleModule.scenarios.platform

import android.content.Context
import android.net.Uri
import com.rsicarelli.fakt.Fake

/**
 * Android framework types (`Context`, `Uri`) in `@Fake` signatures. The generation task
 * analyses them against the Android SDK boot classpath, which KGP leaves out of the compilation's
 * dependencies (issue #158).
 */
@Fake
interface AppInfoProvider {
    fun appName(context: Context): String

    fun deepLink(path: String): Uri?

    fun versionCode(): Int
}
