// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import dev.rsicarelli.ktfake.Fake

@Fake
interface TestService {
    val memes: String
    fun getValue(): String
    fun setValue(value: String)
}

@Fake(trackCalls = true)
interface AnalyticsService {
    fun track(event: String)
}

@Fake
interface AsyncUserService {
    suspend fun getUser(id: String): String
    suspend fun updateUser(id: String, name: String): Boolean
    suspend fun deleteUser(id: String)
}
