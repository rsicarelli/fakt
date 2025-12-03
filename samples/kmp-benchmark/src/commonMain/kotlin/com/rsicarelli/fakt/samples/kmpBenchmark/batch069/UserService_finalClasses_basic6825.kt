// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch069

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpBenchmark.models.User

@Fake
open class UserService_finalClasses_basic6825 {
    open fun getUser(id: String): User {
        
        return User(id, "John Doe", "john@example.com")
    }

    open fun saveUser(user: User) {
        
        println("Saving user: ${user.name}")
    }

    open fun deleteUser(id: String): Boolean {
        
        return true
    }

    
    fun validateUserId(id: String): Boolean = id.isNotEmpty() && id.length <= 100
}
