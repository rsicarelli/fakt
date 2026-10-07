// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpAllJvm

import com.rsicarelli.fakt.Fake

/** A user known to the app. */
data class User(val id: String, val name: String)

/** Common repository. Its fake is made by `faktGenerateCommonMain` and used in commonTest. */
@Fake
interface UserRepository {
    /** The name of the signed-in user. */
    val currentUserName: String

    /** Loads a user by id, or null when it doesn't exist. */
    suspend fun find(id: String): User?
}

/** Resolved per target; the `actual` lives in desktopMain and in serverMain. */
expect fun platformName(): String

/** Common logic under test in commonTest. */
class Greeter(private val repository: UserRepository) {
    /** Says hello to user [id], naming the platform it runs on. */
    suspend fun greet(id: String): String {
        val user = repository.find(id)
        return "Hello, ${user?.name ?: repository.currentUserName} from ${platformName()}"
    }
}
