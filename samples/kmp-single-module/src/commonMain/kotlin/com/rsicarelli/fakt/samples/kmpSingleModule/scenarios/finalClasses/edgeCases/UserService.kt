// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.edgeCases

import com.rsicarelli.fakt.Fake

/**
 * Scenario: NullableHandling
 * Tests nullable types in parameters and return types
 */
@Fake
open class UserService {
    open fun findById(id: String?): User? = null

    open fun update(user: User?): Boolean = false

    open fun merge(
        primary: User?,
        secondary: User?,
    ): User? = primary

    open val currentUser: User?
        get() = null

    open var cachedUser: User? = null
}

data class User(
    val id: String,
    val name: String,
)
