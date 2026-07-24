// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark.domain

import com.rsicarelli.fakt.Fake

/**
 * Fakt's copy of the shared benchmark domain. This file is byte-identical to
 * `contracts/.../benchmark/domain/Domain.kt` except for the `@Fake` annotations and this import —
 * a compiler plugin must see `@Fake` at the declaration site, so Fakt cannot consume the plain
 * `:contracts` copy. A CI diff check keeps the two in lock-step.
 */

/** Simple value type flowing through the domain. */
data class User(
    val id: String,
    val name: String,
    val email: String = "",
    val age: Int = 0,
)

/** Suspend-heavy repository — exercises coroutine stubbing (mocks pay extra wrapping). */
@Fake
interface UserRepository {
    suspend fun getUser(id: String): User?

    suspend fun saveUser(user: User): Result<Unit>

    suspend fun listUsers(page: Int): List<User>
}

/** High-frequency void calls — the surface for interaction verification (the 47x `verify` hot-spot). */
@Fake
interface AnalyticsService {
    fun track(event: String)

    fun identify(userId: String)
}

/** Property-heavy configuration — exercises property stubbing. */
@Fake
interface ConfigProvider {
    val baseUrl: String

    val timeout: Long

    val retryCount: Int?

    val featureFlags: Map<String, Boolean>
}

/** Generic use case — exercises generic method stubbing. */
@Fake
interface UseCase<in P, out R> {
    suspend fun execute(param: P): R
}

/**
 * The "deep" interface: method-level generics, suspend, varargs and properties in one type — the
 * maximum instrumentation surface, where reflection/bytecode mocks pay the most.
 */
@Fake
interface ComplexApiService {
    fun simpleMethod(): String

    suspend fun asyncMethod(): String

    fun <T> genericMethod(value: T): T

    val readOnlyProperty: String

    var mutableProperty: Int

    fun varargsMethod(vararg values: String): Int

    suspend fun <T> asyncGenericMethod(value: T): T
}
