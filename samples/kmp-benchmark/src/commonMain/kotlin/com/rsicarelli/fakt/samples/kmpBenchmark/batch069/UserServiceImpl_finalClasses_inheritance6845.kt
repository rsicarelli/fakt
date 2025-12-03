// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch069

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpBenchmark.models.User

interface UserServiceImpl_finalClasses_inheritance6845 {
    fun getUser(id: String): User

    fun saveUser(user: User)
}

@Fake
open class UserServiceImpl_finalClasses_inheritance6845_1 : UserServiceImpl_finalClasses_inheritance6845 {
    
    override fun getUser(id: String): User {
        
        return User(id, "User $id", "user$id@example.com")
    }

    override fun saveUser(user: User) {
        
    }

    
    open fun validateUser(user: User): Boolean = user.id.isNotEmpty() && user.name.isNotEmpty()

    open fun logOperation(operation: String) {
        println("Operation: $operation")
    }
}
