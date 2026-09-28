// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compatagp

import com.rsicarelli.fakt.Fake

/**
 * `@Fake` interface exercised by every AGP-compat sample. Fakt generates the fake into the Android
 * `testFixtures` source set (enabled per sample), and the module's own unit test consumes it —
 * validating the whole Android test-fixtures pipeline against a specific AGP version.
 */
@Fake
interface UserRepository {
    val currentUser: String

    fun getUser(id: Long): String

    fun findUsers(query: String, limit: Int): List<String>
}
