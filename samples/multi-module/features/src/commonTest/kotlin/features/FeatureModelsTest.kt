// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package features

import domain.AuthToken
import domain.Order
import domain.OrderStatus
import domain.User
import domain.fakeAuthenticationService
import domain.fakeOrderService
import domain.fakeUserRepository
import foundation.LogLevel
import foundation.fakeLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance

/**
 * Features module tests - Transitive cross-module validation
 *
 * Critical validation:
 * - Import fakes from domain module (fakeUserRepository, fakeOrderService, fakeAuthenticationService)
 * - Import fakes from foundation module transitively (fakeLogger)
 * - Verify transitive dependency resolution
 * - Validate complex multi-module dependency graphs
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FeatureModelsTest {

    @Test
    fun `GIVEN UserFeature fake WHEN using domain and foundation fakes THEN should handle multi-module dependencies`() =
        runTest {
            // Given - Create fakes from domain and foundation (cross-module imports)
            val logger =
                fakeLogger {
                    info { message, tag -> }
                }

            val userRepository =
                fakeUserRepository {
                    getUser { id, loggerParam ->
                        loggerParam.info("Repository: Getting user $id")
                        Result.success(
                            User(
                                id = id,
                                name = "John Doe",
                                email = "john@example.com",
                                roles = listOf("admin"),
                            ),
                        )
                    }
                }

            val userFeature =
                fakeUserFeature {
                    getUserProfile { userId, repository, loggerParam ->
                        loggerParam.info("Feature: Getting profile for $userId")
                        val userResult = repository.getUser(userId, loggerParam)
                        userResult.map { user ->
                            UserProfile(
                                user = user,
                                preferences = mapOf("theme" to "dark"),
                                lastLogin = System.currentTimeMillis(),
                            )
                        }
                    }
                }

            // When - Use domain and foundation fakes with features
            val result = userFeature.getUserProfile("user123", userRepository, logger)

            // Then
            assertNotNull(result.getOrNull())
            assertEquals("user123", result.getOrNull()?.user?.id)
            assertEquals("John Doe", result.getOrNull()?.user?.name)
            assertEquals("dark", result.getOrNull()?.preferences?.get("theme"))
        }

    @Test
    fun `GIVEN OrderFeature fake WHEN using multiple domain fakes THEN should coordinate between services`() =
        runTest {
            // Given - Create multiple domain fakes (cross-module imports)
            val logger =
                fakeLogger {
                    info { message, tag -> }
                    minLogLevel { LogLevel.INFO }
                }

            val userRepository =
                fakeUserRepository {
                    getUser { id, loggerParam ->
                        Result.success(
                            User(
                                id = id,
                                name = "Jane Doe",
                                email = "jane@example.com",
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
                                id = "order456",
                                userId = userId,
                                items = items,
                                status = OrderStatus.PENDING,
                                total = items.sumOf { it.price * it.quantity },
                            ),
                        )
                    }
                }

            val orderFeature =
                fakeOrderFeature {
                    placeOrder { userId, orderDetails, userRepo, orderSvc, loggerParam ->
                        // Verify user exists
                        val userResult = userRepo.getUser(userId, loggerParam)
                        if (userResult.isFailure) return@placeOrder Result.failure(userResult.exceptionOrNull()!!)

                        // Create order
                        val orderItems =
                            orderDetails.items.map {
                                domain.OrderItem(
                                    productId = it.productId,
                                    quantity = it.quantity,
                                    price = 10.0,
                                )
                            }
                        orderSvc.createOrder(userId, orderItems, loggerParam)
                    }
                }

            val orderDetails =
                OrderDetails(
                    items = listOf(OrderItemDetails("product1", 2), OrderItemDetails("product2", 1)),
                    shippingAddress = "123 Main St",
                    paymentMethod = "credit_card",
                )

            // When - Coordinate multiple domain services
            val result = orderFeature.placeOrder("user123", orderDetails, userRepository, orderService, logger)

            // Then
            assertNotNull(result.getOrNull())
            assertEquals("order456", result.getOrNull()?.id)
            assertEquals("user123", result.getOrNull()?.userId)
            assertEquals(OrderStatus.PENDING, result.getOrNull()?.status)
        }

    @Test
    fun `GIVEN AuthenticationFeature fake WHEN using domain authentication and user services THEN should create auth session`() =
        runTest {
            // Given - Create domain fakes (cross-module imports)
            val logger =
                fakeLogger {
                    info { message, tag -> }
                }

            val authService =
                fakeAuthenticationService {
                    login { username, password, loggerParam ->
                        loggerParam.info("Authenticating user: $username")
                        Result.success(
                            AuthToken(
                                value = "token-$username",
                                expiresAt = System.currentTimeMillis() + 3600000,
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
                                name = username.removePrefix("user-"),
                                email = "${username.removePrefix("user-")}@example.com",
                                roles = listOf("user"),
                            ),
                        )
                    }
                }

            val authFeature =
                fakeAuthenticationFeature {
                    authenticateUser { username, password, authSvc, userRepo, loggerParam ->
                        // Authenticate
                        val tokenResult = authSvc.login(username, password, loggerParam)
                        if (tokenResult.isFailure) return@authenticateUser Result.failure(tokenResult.exceptionOrNull()!!)

                        val token = tokenResult.getOrNull()!!

                        // Get user details
                        val userResult = userRepo.getUser(token.userId, loggerParam)
                        if (userResult.isFailure) return@authenticateUser Result.failure(userResult.exceptionOrNull()!!)

                        Result.success(
                            AuthSession(
                                token = token,
                                user = userResult.getOrNull()!!,
                                expiresAt = token.expiresAt,
                            ),
                        )
                    }
                }

            // When - Use multiple domain services to create session
            val result =
                authFeature.authenticateUser(
                    "testuser",
                    "password123",
                    authService,
                    userRepository,
                    logger,
                )

            // Then
            assertNotNull(result.getOrNull())
            assertEquals("token-testuser", result.getOrNull()?.token?.value)
            assertEquals("user-testuser", result.getOrNull()?.user?.id)
            assertEquals("testuser", result.getOrNull()?.user?.name)
        }
}
