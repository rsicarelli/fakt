// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compatagp

import android.content.Context
import com.rsicarelli.fakt.Fake

/**
 * An `@Fake` whose signature uses an Android framework type.
 *
 * This cell uses AGP 9's built-in Kotlin, which Fakt still generates for inside `compileKotlin*`
 * (issue #154), so it does NOT exercise the generation task's Android SDK boot classpath (issue
 * #158). That is covered by the KGP-applied cells (agp-8.11, agp-8.12, agp-9.4). Here it locks the
 * behaviour the task path must keep once #154 moves built-in Kotlin onto it.
 */
@Fake
interface AppInfoProvider {
    fun appName(context: Context): String

    fun versionCode(): Int
}
