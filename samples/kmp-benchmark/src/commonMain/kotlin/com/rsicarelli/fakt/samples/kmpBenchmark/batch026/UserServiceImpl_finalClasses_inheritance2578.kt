// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch026

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpBenchmark.models.User

interface UserServiceImpl_finalClasses_inheritance2578 {
    fun getUser(id: String): User

    fun saveUser(user: User)
}

@Fake
open class UserServiceImpl_finalClasses_inheritance2578_1 : UserServiceImpl_finalClasses_inheritance2578 {
    
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
