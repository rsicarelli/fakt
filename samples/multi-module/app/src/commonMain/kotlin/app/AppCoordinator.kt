// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package app

import com.rsicarelli.fakt.Fake
import domain.AuthenticationService
import domain.OrderService
import domain.UserRepository
import features.AuthenticationFeature
import features.OrderDetails
import features.OrderFeature
import features.UserFeature
import foundation.ConfigService
import foundation.Logger
import foundation.NetworkClient

// ============================================================================
// APP MODULE - Top-level coordination
// Dependency chain: foundation → domain → features → app
// ============================================================================

/**
 * Application coordinator - App layer
 *
 * Complete dependency chain validation:
 * - Uses features from features module (UserFeature, OrderFeature, AuthenticationFeature)
 * - Uses domain services from domain module (UserRepository, OrderService, AuthenticationService)
 * - Uses foundation utilities from foundation module (Logger, ConfigService, NetworkClient)
 * - App tests must import ALL fakes from ALL modules
 * - Validates complete cross-module fake accessibility
 */
@Fake
interface AppCoordinator {
    suspend fun initializeApp(
        logger: Logger,
        config: ConfigService,
        networkClient: NetworkClient,
    ): Result<AppState>

    suspend fun handleUserLogin(
        username: String,
        password: String,
        authFeature: AuthenticationFeature,
        authService: AuthenticationService,
        userRepository: UserRepository,
        logger: Logger,
    ): Result<UserSession>

    suspend fun handleOrderPlacement(
        userId: String,
        orderDetails: OrderDetails,
        orderFeature: OrderFeature,
        orderService: OrderService,
        userRepository: UserRepository,
        logger: Logger,
    ): Result<OrderConfirmation>

    suspend fun syncData(
        userFeature: UserFeature,
        orderService: OrderService,
        networkClient: NetworkClient,
        logger: Logger,
    ): Result<SyncResult>
}

data class AppState(
    val initialized: Boolean,
    val version: String,
    val environment: String,
)

data class UserSession(
    val sessionId: String,
    val userId: String,
    val userName: String,
    val expiresAt: Long,
)

data class OrderConfirmation(
    val orderId: String,
    val userId: String,
    val totalAmount: Double,
    val estimatedDelivery: String,
)

data class SyncResult(
    val usersSynced: Int,
    val ordersSynced: Int,
    val lastSyncTime: Long,
)
