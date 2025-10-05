// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.companionObjects.basic

import com.rsicarelli.fakt.Fake

/**
 * Scenario 1: Interface with companion factory method
 *
 * Companion objects are commonly used for factory methods and constants.
 */
@Fake
interface UserRepository {
    fun getUser(id: String): Result<User>

    companion object {
        fun create(config: String): UserRepository {
            // Factory method implementation
            TODO("Not needed for fake generation")
        }

        const val DEFAULT_TIMEOUT = 5000L
    }
}

data class User(val id: String, val name: String)
