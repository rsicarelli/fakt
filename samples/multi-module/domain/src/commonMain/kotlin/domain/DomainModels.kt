// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package domain

import com.rsicarelli.fakt.Fake
import foundation.ConfigService
import foundation.Logger
import foundation.NetworkResponse

// ============================================================================
// DOMAIN MODULE - Business logic layer
// Dependency chain: foundation → domain → features → app
// ============================================================================

/**
 * User repository - Domain layer
 *
 * Cross-module validation:
 * - Uses Logger from foundation module
 * - Uses ConfigService from foundation module
 * - Domain tests must import fakeLogger() and fakeConfigService()
 * - Features module must transitively access these foundation fakes
 */
@Fake
interface UserRepository {
    suspend fun getUser(id: String, logger: Logger): Result<User>

    suspend fun saveUser(user: User, logger: Logger): Result<Unit>

    suspend fun deleteUser(id: String, logger: Logger): Result<Unit>

    fun configure(config: ConfigService): RepositoryConfig
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val roles: List<String>,
)

data class RepositoryConfig(
    val maxRetries: Int,
    val timeout: Long,
    val cacheEnabled: Boolean,
)

/**
 * Order service - Domain layer
 *
 * Cross-module validation:
 * - Uses Logger from foundation module
 * - Uses NetworkResponse from foundation module (via NetworkClient)
 * - Complex return types across modules
 * - Suspend functions with cross-module dependencies
 */
@Fake
interface OrderService {
    suspend fun createOrder(
        userId: String,
        items: List<OrderItem>,
        logger: Logger,
    ): Result<Order>

    suspend fun getOrder(orderId: String, logger: Logger): Result<Order>

    suspend fun cancelOrder(orderId: String, logger: Logger): Result<Unit>

    suspend fun syncWithBackend(logger: Logger): Result<NetworkResponse>

    val config: OrderConfig
}

data class Order(
    val id: String,
    val userId: String,
    val items: List<OrderItem>,
    val status: OrderStatus,
    val total: Double,
)

data class OrderItem(
    val productId: String,
    val quantity: Int,
    val price: Double,
)

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
}

data class OrderConfig(
    val allowCancellation: Boolean,
    val maxItems: Int,
    val currency: String,
)

/**
 * Authentication service - Domain layer
 *
 * Cross-module validation:
 * - Uses Logger from foundation module
 * - Uses ConfigService from foundation module
 * - Multiple cross-module dependencies in single interface
 * - Tests validation of complex dependency graphs
 */
@Fake
interface AuthenticationService {
    suspend fun login(
        username: String,
        password: String,
        logger: Logger,
    ): Result<AuthToken>

    suspend fun logout(token: AuthToken, logger: Logger): Result<Unit>

    suspend fun refreshToken(
        token: AuthToken,
        config: ConfigService,
        logger: Logger,
    ): Result<AuthToken>

    fun validateToken(token: AuthToken): Boolean
}

data class AuthToken(
    val value: String,
    val expiresAt: Long,
    val userId: String,
)
