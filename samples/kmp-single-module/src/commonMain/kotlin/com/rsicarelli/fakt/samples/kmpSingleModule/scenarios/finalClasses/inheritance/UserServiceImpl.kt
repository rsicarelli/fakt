// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.inheritance

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpSingleModule.models.User

/**
 * P1 Scenario: ClassImplementingInterface
 *
 * **Pattern**: Open class implementing interface with additional methods
 * **Priority**: P1 (High - Common Implementation Pattern)
 *
 * **What it tests**:
 * - Open class implementing interface can be faked
 * - Interface methods have NO default implementation (must configure)
 * - Own open methods have super call defaults (optional override)
 * - Distinction between interface contract vs class implementation
 *
 * **Expected behavior**:
 * ```kotlin
 * // Interface methods - no super, must configure
 * private var getUserBehavior: (String) -> User = { _ -> error("Configure getUser behavior") }
 * private var saveUserBehavior: (User) -> Unit = { _ -> error("Configure saveUser behavior") }
 *
 * // Own open methods - have super
 * private var validateUserBehavior: (User) -> Boolean = { user -> super.validateUser(user) }
 * private var logOperationBehavior: (String) -> Unit = { op -> super.logOperation(op) }
 * ```
 *
 * **Real-world equivalent**:
 * ```kotlin
 * interface UserService {
 *     fun getUser(id: String): User
 *     fun saveUser(user: User)
 * }
 *
 * open class UserServiceImpl : UserService {
 *     override fun getUser(id: String): User = TODO()
 *     override fun saveUser(user: User) = TODO()
 *     open fun validateUser(user: User): Boolean = user.id.isNotEmpty()
 *     open fun logOperation(operation: String) = println(operation)
 * }
 * ```
 */

// Interface contract
interface UserService {
    fun getUser(id: String): User

    fun saveUser(user: User)
}

// Open class implementing interface with additional methods
@Fake
open class UserServiceImpl : UserService {
    // Interface implementations - must be provided
    override fun getUser(id: String): User {
        // Simulate database query
        return User(id, "User $id", "user$id@example.com")
    }

    override fun saveUser(user: User) {
        // Simulate saving to database
    }

    // Own open methods - have default implementation
    open fun validateUser(user: User): Boolean = user.id.isNotEmpty() && user.name.isNotEmpty()

    open fun logOperation(operation: String) {
        println("Operation: $operation")
    }
}
