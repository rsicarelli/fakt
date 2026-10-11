// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpandroidtarget.shared

import com.rsicarelli.fakt.Fake

/** Common fake: reaches commonTest, jvmTest and every Android unit-test variant. */
@Fake
interface UserRepository {
    fun findName(id: String): String
}

/** Resolved per target: androidMain and jvmMain each provide an actual. */
expect fun platformName(): String
