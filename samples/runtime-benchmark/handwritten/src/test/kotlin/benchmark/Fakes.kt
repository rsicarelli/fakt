// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package benchmark

import benchmark.domain.AnalyticsService
import benchmark.domain.ComplexApiService
import benchmark.domain.ConfigProvider
import benchmark.domain.User
import benchmark.domain.UserRepository

/**
 * Hand-written fakes — plain Kotlin classes, the baseline every generated/reflected competitor is
 * compared against. Call counting is a simple counter (the classic "manual fake" pattern the Fakt
 * docs describe as tedious-but-fast).
 */

class FakeAnalyticsService : AnalyticsService {
    var trackCount: Int = 0
    var identifyCount: Int = 0

    override fun track(event: String) {
        trackCount++
    }

    override fun identify(userId: String) {
        identifyCount++
    }
}

class FakeConfigProvider(
    override val baseUrl: String = "",
    override val timeout: Long = 0L,
    override val retryCount: Int? = null,
    override val featureFlags: Map<String, Boolean> = emptyMap(),
) : ConfigProvider

class FakeComplexApiService(
    private val simple: String = "",
    override val readOnlyProperty: String = "",
    override var mutableProperty: Int = 0,
) : ComplexApiService {
    override fun simpleMethod(): String = simple

    override suspend fun asyncMethod(): String = ""

    override fun <T> genericMethod(value: T): T = value

    override fun varargsMethod(vararg values: String): Int = values.size

    override suspend fun <T> asyncGenericMethod(value: T): T = value
}

class FakeUserRepository(
    private val onGet: (String) -> User? = { null },
    private val onSave: () -> Result<Unit> = { Result.success(Unit) },
    private val onList: () -> List<User> = { emptyList() },
) : UserRepository {
    override suspend fun getUser(id: String): User? = onGet(id)

    override suspend fun saveUser(user: User): Result<Unit> = onSave()

    override suspend fun listUsers(page: Int): List<User> = onList()
}
