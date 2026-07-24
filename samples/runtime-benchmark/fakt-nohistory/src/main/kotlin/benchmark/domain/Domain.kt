// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark.domain

import com.rsicarelli.fakt.CallHistoryMode
import com.rsicarelli.fakt.Fake

/**
 * Byte-identical to the :fakt domain except every interface disables call-history tracking
 * (`@Fake(callHistory = CallHistoryMode.DISABLED)`). The generated fakes therefore skip the
 * MutableStateFlow recording on every call — no `xxxCalls` property is generated — so the delta
 * against the :fakt column is exactly the call-history cost.
 */

data class User(
    val id: String,
    val name: String,
    val email: String = "",
    val age: Int = 0,
)

@Fake(callHistory = CallHistoryMode.DISABLED)
interface UserRepository {
    suspend fun getUser(id: String): User?

    suspend fun saveUser(user: User): Result<Unit>

    suspend fun listUsers(page: Int): List<User>
}

@Fake(callHistory = CallHistoryMode.DISABLED)
interface AnalyticsService {
    fun track(event: String)

    fun identify(userId: String)
}

@Fake(callHistory = CallHistoryMode.DISABLED)
interface ConfigProvider {
    val baseUrl: String

    val timeout: Long

    val retryCount: Int?

    val featureFlags: Map<String, Boolean>
}

@Fake(callHistory = CallHistoryMode.DISABLED)
interface UseCase<in P, out R> {
    suspend fun execute(param: P): R
}

@Fake(callHistory = CallHistoryMode.DISABLED)
interface ComplexApiService {
    fun simpleMethod(): String

    suspend fun asyncMethod(): String

    fun <T> genericMethod(value: T): T

    val readOnlyProperty: String

    var mutableProperty: Int

    fun varargsMethod(vararg values: String): Int

    suspend fun <T> asyncGenericMethod(value: T): T
}
