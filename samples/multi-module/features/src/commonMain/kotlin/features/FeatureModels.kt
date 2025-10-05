// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package features

import com.rsicarelli.fakt.Fake
import domain.AuthToken
import domain.AuthenticationService
import domain.Order
import domain.OrderService
import domain.User
import domain.UserRepository
import foundation.Logger

// ============================================================================
// FEATURES MODULE - Feature layer
// Dependency chain: foundation → domain → features → app
// ============================================================================

/**
 * User feature - Feature layer
 *
 * Cross-module validation:
 * - Uses UserRepository from domain module
 * - Uses Logger from foundation module (transitive)
 * - Features tests must import fakeUserRepository() from domain
 * - Features tests must import fakeLogger() from foundation (transitive)
 * - App module must access all three layers' fakes
 */
@Fake
interface UserFeature {
    suspend fun getUserProfile(
        userId: String,
        repository: UserRepository,
        logger: Logger,
    ): Result<UserProfile>

    suspend fun updateUserProfile(
        userId: String,
        profile: UserProfile,
        repository: UserRepository,
        logger: Logger,
    ): Result<Unit>

    suspend fun deleteUserAccount(
        userId: String,
        repository: UserRepository,
        logger: Logger,
    ): Result<Unit>
}

data class UserProfile(
    val user: User,
    val preferences: Map<String, String>,
    val lastLogin: Long,
)

/**
 * Order feature - Feature layer
 *
 * Cross-module validation:
 * - Uses OrderService from domain module
 * - Uses UserRepository from domain module
 * - Uses Logger from foundation module (transitive)
 * - Multiple domain dependencies in single feature
 */
@Fake
interface OrderFeature {
    suspend fun placeOrder(
        userId: String,
        orderDetails: OrderDetails,
        userRepository: UserRepository,
        orderService: OrderService,
        logger: Logger,
    ): Result<Order>

    suspend fun trackOrder(
        orderId: String,
        orderService: OrderService,
        logger: Logger,
    ): Result<OrderTracking>

    suspend fun cancelOrder(
        orderId: String,
        userId: String,
        orderService: OrderService,
        userRepository: UserRepository,
        logger: Logger,
    ): Result<Unit>
}

data class OrderDetails(
    val items: List<OrderItemDetails>,
    val shippingAddress: String,
    val paymentMethod: String,
)

data class OrderItemDetails(
    val productId: String,
    val quantity: Int,
)

data class OrderTracking(
    val order: Order,
    val currentStatus: String,
    val estimatedDelivery: Long?,
    val trackingNumber: String?,
)

/**
 * Authentication feature - Feature layer
 *
 * Cross-module validation:
 * - Uses AuthenticationService from domain module
 * - Uses UserRepository from domain module
 * - Uses Logger from foundation module (transitive)
 * - Complex multi-module dependency graph
 */
@Fake
interface AuthenticationFeature {
    suspend fun authenticateUser(
        username: String,
        password: String,
        authService: AuthenticationService,
        userRepository: UserRepository,
        logger: Logger,
    ): Result<AuthSession>

    suspend fun refreshSession(
        session: AuthSession,
        authService: AuthenticationService,
        logger: Logger,
    ): Result<AuthSession>

    suspend fun logout(
        session: AuthSession,
        authService: AuthenticationService,
        logger: Logger,
    ): Result<Unit>
}

data class AuthSession(
    val token: AuthToken,
    val user: User,
    val expiresAt: Long,
)
