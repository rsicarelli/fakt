// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.companionObjects.basic

import com.rsicarelli.fakt.Fake

/**
 * Scenario 1: Interface with companion factory method
 *
 * Companion objects are commonly used for factory methods and constants.
 */
@Fake
interface UserRepository {
    fun getUser(id: String): Result<User>
}

data class User(
    val id: String,
    val name: String,
)
