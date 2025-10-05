// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package app

import domain.AuthToken
import domain.Order
import domain.OrderItem
import domain.OrderStatus
import domain.User
import domain.fakeAuthenticationService
import domain.fakeOrderService
import domain.fakeUserRepository
import features.AuthSession
import features.OrderDetails
import features.OrderItemDetails
import features.UserProfile
import features.fakeAuthenticationFeature
import features.fakeOrderFeature
import features.fakeUserFeature
import foundation.LogLevel
import foundation.NetworkResponse
import foundation.fakeConfigService
import foundation.fakeLogger
import foundation.fakeNetworkClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance

/**
 * App module tests - Complete dependency chain validation
 *
 * Ultimate cross-module validation:
 * - Import fakes from features module (fakeUserFeature, fakeOrderFeature, fakeAuthenticationFeature)
 * - Import fakes from domain module (fakeUserRepository, fakeOrderService, fakeAuthenticationService)
 * - Import fakes from foundation module (fakeLogger, fakeConfigService, fakeNetworkClient)
 * - Validate ALL fakes accessible from top-level app module
 * - Prove complete cross-module fake generation and import resolution
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppCoordinatorTest {

    @Test
    fun `GIVEN AppCoordinator WHEN initializing app THEN should use all foundation fakes`() =
        runTest {
            // Given - Create all foundation fakes (cross-module imports)
            val logger =
                fakeLogger {
                    info { message, tag -> }
                    minLogLevel { LogLevel.INFO }
                }

            val configService =
                fakeConfigService {
                    getString { key, default ->
                        when (key) {
                            "app.version" -> "1.0.0"
                            "app.environment" -> "production"
                            else -> default
                        }
                    }
                }

            val networkClient =
                fakeNetworkClient {
                    baseUrl { "https://api.production.com" }
                    timeout { 10000L }
                }

            val appCoordinator =
                fakeAppCoordinator {
                    initializeApp { loggerParam, config, network ->
                        loggerParam.info("Initializing app")
                        Result.success(
                            AppState(
                                initialized = true,
                                version = config.getString("app.version", "unknown"),
                                environment = config.getString("app.environment", "development"),
                            ),
                        )
                    }
                }

            // When - Use all foundation fakes
            val result = appCoordinator.initializeApp(logger, configService, networkClient)

            // Then
            assertNotNull(result.getOrNull())
            assertEquals(true, result.getOrNull()?.initialized)
            assertEquals("1.0.0", result.getOrNull()?.version)
            assertEquals("production", result.getOrNull()?.environment)
        }

    @Test
    fun `GIVEN AppCoordinator WHEN handling user login THEN should use features and domain fakes`() =
        runTest {
            // Given - Create features and domain fakes (cross-module imports)
            val logger =
                fakeLogger {
                    info { message, tag -> }
                }

            val authService =
                fakeAuthenticationService {
                    login { username, password, loggerParam ->
                        Result.success(
                            AuthToken(
                                value = "session-$username",
                                expiresAt = System.currentTimeMillis() + 7200000,
                                userId = "user-$username",
                            ),
                        )
                    }
                }

            val userRepository =
                fakeUserRepository {
                    getUser { id, loggerParam ->
                        Result.success(
                            User(
                                id = id,
                                name = "Alice Johnson",
                                email = "alice@example.com",
                                roles = listOf("admin", "user"),
                            ),
                        )
                    }
                }

            val authFeature =
                fakeAuthenticationFeature {
                    authenticateUser { username, password, authSvc, userRepo, loggerParam ->
                        val tokenResult = authSvc.login(username, password, loggerParam)
                        val userResult = userRepo.getUser("user-$username", loggerParam)

                        if (tokenResult.isSuccess && userResult.isSuccess) {
                            Result.success(
                                AuthSession(
                                    token = tokenResult.getOrNull()!!,
                                    user = userResult.getOrNull()!!,
                                    expiresAt = tokenResult.getOrNull()!!.expiresAt,
                                ),
                            )
                        } else {
                            Result.failure(Exception("Authentication failed"))
                        }
                    }
                }

            val appCoordinator =
                fakeAppCoordinator {
                    handleUserLogin { username, password, authFeat, authSvc, userRepo, loggerParam ->
                        val sessionResult =
                            authFeat.authenticateUser(username, password, authSvc, userRepo, loggerParam)
                        sessionResult.map { session ->
                            UserSession(
                                sessionId = session.token.value,
                                userId = session.user.id,
                                userName = session.user.name,
                                expiresAt = session.expiresAt,
                            )
                        }
                    }
                }

            // When - Use features, domain, and foundation fakes together
            val result =
                appCoordinator.handleUserLogin(
                    "alice",
                    "password123",
                    authFeature,
                    authService,
                    userRepository,
                    logger,
                )

            // Then
            assertNotNull(result.getOrNull())
            assertEquals("session-alice", result.getOrNull()?.sessionId)
            assertEquals("user-alice", result.getOrNull()?.userId)
            assertEquals("Alice Johnson", result.getOrNull()?.userName)
        }

    @Test
    fun `GIVEN AppCoordinator WHEN handling order placement THEN should coordinate all layers`() =
        runTest {
            // Given - Create all layer fakes (complete dependency chain)
            val logger =
                fakeLogger {
                    info { message, tag -> }
                }

            val userRepository =
                fakeUserRepository {
                    getUser { id, loggerParam ->
                        Result.success(
                            User(
                                id = id,
                                name = "Bob Smith",
                                email = "bob@example.com",
                                roles = listOf("user"),
                            ),
                        )
                    }
                }

            val orderService =
                fakeOrderService {
                    createOrder { userId, items, loggerParam ->
                        Result.success(
                            Order(
                                id = "order789",
                                userId = userId,
                                items = items,
                                status = OrderStatus.CONFIRMED,
                                total = items.sumOf { it.price * it.quantity },
                            ),
                        )
                    }
                }

            val orderFeature =
                fakeOrderFeature {
                    placeOrder { userId, orderDetails, userRepo, orderSvc, loggerParam ->
                        val userResult = userRepo.getUser(userId, loggerParam)
                        if (userResult.isFailure) return@placeOrder Result.failure(userResult.exceptionOrNull()!!)

                        val orderItems =
                            orderDetails.items.map {
                                OrderItem(
                                    productId = it.productId,
                                    quantity = it.quantity,
                                    price = 25.0,
                                )
                            }
                        orderSvc.createOrder(userId, orderItems, loggerParam)
                    }
                }

            val appCoordinator =
                fakeAppCoordinator {
                    handleOrderPlacement { userId, orderDetails, orderFeat, orderSvc, userRepo, loggerParam ->
                        val orderResult =
                            orderFeat.placeOrder(userId, orderDetails, userRepo, orderSvc, loggerParam)
                        orderResult.map { order ->
                            OrderConfirmation(
                                orderId = order.id,
                                userId = order.userId,
                                totalAmount = order.total,
                                estimatedDelivery = "3-5 business days",
                            )
                        }
                    }
                }

            val orderDetails =
                OrderDetails(
                    items = listOf(OrderItemDetails("laptop", 1), OrderItemDetails("mouse", 2)),
                    shippingAddress = "456 Oak Ave",
                    paymentMethod = "paypal",
                )

            // When - Coordinate features, domain, and foundation layers
            val result =
                appCoordinator.handleOrderPlacement(
                    "user123",
                    orderDetails,
                    orderFeature,
                    orderService,
                    userRepository,
                    logger,
                )

            // Then
            assertNotNull(result.getOrNull())
            assertEquals("order789", result.getOrNull()?.orderId)
            assertEquals("user123", result.getOrNull()?.userId)
            assertEquals(75.0, result.getOrNull()?.totalAmount) // (1*25 + 2*25)
        }

    @Test
    fun `GIVEN AppCoordinator WHEN syncing data THEN should use all module fakes`() =
        runTest {
            // Given - Complete set of fakes from all modules
            val logger =
                fakeLogger {
                    info { message, tag -> }
                }

            val networkClient =
                fakeNetworkClient {
                    get { url, headers ->
                        Result.success(
                            NetworkResponse(
                                statusCode = 200,
                                body = "{\"status\":\"synced\"}",
                                headers = mapOf("Content-Type" to "application/json"),
                            ),
                        )
                    }
                }

            val userFeature =
                fakeUserFeature {
                    getUserProfile { userId, repository, loggerParam ->
                        Result.success(
                            UserProfile(
                                user =
                                    User(
                                        id = userId,
                                        name = "Test User",
                                        email = "test@example.com",
                                        roles = listOf("user"),
                                    ),
                                preferences = mapOf(),
                                lastLogin = System.currentTimeMillis(),
                            ),
                        )
                    }
                }

            val orderService =
                fakeOrderService {
                    syncWithBackend { loggerParam ->
                        Result.success(
                            NetworkResponse(
                                statusCode = 200,
                                body = "{\"orders_synced\":10}",
                                headers = mapOf(),
                            ),
                        )
                    }
                }

            val appCoordinator =
                fakeAppCoordinator {
                    syncData { userFeat, orderSvc, network, loggerParam ->
                        loggerParam.info("Starting data sync")

                        val backendResponse = orderSvc.syncWithBackend(loggerParam)
                        if (backendResponse.isSuccess) {
                            Result.success(
                                SyncResult(
                                    usersSynced = 5,
                                    ordersSynced = 10,
                                    lastSyncTime = System.currentTimeMillis(),
                                ),
                            )
                        } else {
                            Result.failure(Exception("Sync failed"))
                        }
                    }
                }

            // When - Use fakes from all three modules
            val result = appCoordinator.syncData(userFeature, orderService, networkClient, logger)

            // Then
            assertNotNull(result.getOrNull())
            assertEquals(5, result.getOrNull()?.usersSynced)
            assertEquals(10, result.getOrNull()?.ordersSynced)
        }
}
