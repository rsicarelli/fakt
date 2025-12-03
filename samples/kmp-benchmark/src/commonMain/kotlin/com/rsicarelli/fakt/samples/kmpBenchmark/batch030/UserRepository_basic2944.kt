// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch030

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpBenchmark.models.User

@Fake
interface UserRepository_basic2944 {
    val users: List<User>

    fun findById(id: String): User?

    fun save(user: User): User

    fun delete(id: String): Boolean

    fun findByAge(
        minAge: Int,
        maxAge: Int = 100,
    ): List<User>
}
