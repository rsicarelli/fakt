// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.kmpandroidlint

import com.rsicarelli.fakt.Fake

/**
 * A `@Fake` interface declared in `commonMain`. Fakt generates its fake into the `commonTest`
 * generated source dir, which the Android `androidHostTest` source set sees and AGP
 * `lintAnalyzeAndroidHostTest` walks — the shape from issue #129. This sample exercises that
 * pipeline end to end under Gradle 9.6.1 (a smoke test); the regression itself is guarded by the
 * `SimplifiedSourceSetConfigurationTest` unit test.
 */
@Fake
interface UserRepository {
    val currentUser: String

    fun getUser(id: Long): String

    fun findUsers(query: String, limit: Int): List<String>
}
