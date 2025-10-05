// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.singleModule.models.User

/**
 * Example: Open class with open methods
 * This is the most common scenario - a concrete class with some overridable behavior.
 */
@Fake
open class UserService {
    open fun getUser(id: String): User {
        // Real implementation would query database
        return User(id, "John Doe", "john@example.com")
    }

    open fun saveUser(user: User) {
        // Real implementation would persist to database
        println("Saving user: ${user.name}")
    }

    open fun deleteUser(id: String): Boolean {
        // Real implementation would delete from database
        return true
    }

    // Final method - cannot be overridden, not included in fake
    fun validateUserId(id: String): Boolean = id.isNotEmpty() && id.length <= 100
}
