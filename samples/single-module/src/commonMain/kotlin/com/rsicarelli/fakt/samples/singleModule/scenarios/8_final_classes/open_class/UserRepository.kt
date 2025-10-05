// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.openClass

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.singleModule.models.User

/**
 * P0 Scenario: Open class with multiple open methods
 *
 * **Pattern**: OpenClassMultipleMethods
 * **Priority**: P0 (Critical)
 *
 * **What it tests**:
 * - Multiple open methods in single class
 * - All methods default to super call
 * - Each method can be independently configured
 *
 * **Expected behavior**:
 * ```kotlin
 * private var findByIdBehavior: (String) -> User? = { id -> super.findById(id) }
 * private var saveBehavior: (User) -> Unit = { user -> super.save(user) }
 * private var deleteBehavior: (String) -> Boolean = { id -> super.delete(id) }
 * private var findAllBehavior: () -> List<User> = { super.findAll() }
 * ```
 */
@Fake
open class UserRepository {
    open fun findById(id: String): User? {
        return null // Default implementation
    }

    open fun save(user: User) {
        println("Saving user: ${user.name}")
    }

    open fun delete(id: String): Boolean = false

    open fun findAll(): List<User> = emptyList()
}
