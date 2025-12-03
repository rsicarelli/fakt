// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch088

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpBenchmark.models.User

@Fake
open class UserRepository_finalClasses_openClass8735 {
    open fun findById(id: String): User? {
        return null 
    }

    open fun save(user: User) {
        println("Saving user: ${user.name}")
    }

    open fun delete(id: String): Boolean = false

    open fun findAll(): List<User> = emptyList()
}
