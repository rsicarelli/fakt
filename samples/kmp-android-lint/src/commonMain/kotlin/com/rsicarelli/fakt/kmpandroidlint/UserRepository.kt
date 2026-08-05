// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.kmpandroidlint

import com.rsicarelli.fakt.Fake

/**
 * A `@Fake` interface declared in `commonMain`. Fakt generates its fake into the `commonTest`
 * generated source dir; the common `commonTest` is visible to the Android `androidHostTest` source
 * set, so AGP `lintAnalyzeAndroidHostTest` walks it. This is the exact shape from issue #129.
 */
@Fake
interface UserRepository {
    val currentUser: String

    fun getUser(id: Long): String

    fun findUsers(query: String, limit: Int): List<String>
}
