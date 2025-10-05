// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package domain

import foundation.LogLevel
import foundation.NetworkResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

/**
 * Domain module tests - Cross-module validation
 *
 * Critical validation:
 * - Import fakes from foundation module (fakeLogger, fakeConfigService)
 * - Use foundation fakes in domain tests
 * - Verify cross-module type resolution (Logger, ConfigService, NetworkResponse)
 */
class DomainModelsTest {

    @Test
    fun `GIVEN UserRepository fake WHEN using foundation Logger THEN should accept Logger from foundation module`() =
        runTest {
            // Given - Create foundation fake (cross-module import)
            val logger =
                fakeLogger {
                    debug { message, tag -> }
                }

            val userRepository =
                fakeUserRepository {
                    getUser { id, loggerParam ->
                        loggerParam.debug("Getting user: $id")
                        Result.success(
                            User(
                                id = id,
                                name = "Test User",
                                email = "test@example.com",
                                roles = listOf("user"),
                            ),
                        )
                    }
                }

            // When - Use foundation Logger with domain UserRepository
            val result = userRepository.getUser("user123", logger)

            // Then
            assertNotNull(result.getOrNull())
            assertEquals("user123", result.getOrNull()?.id)
            assertEquals("Test User", result.getOrNull()?.name)
        }

    @Test
    fun `GIVEN UserRepository fake WHEN using foundation ConfigService THEN should accept ConfigService from foundation module`() {
        // Given - Create foundation fake (cross-module import)
        val configService =
            fakeConfigService {
                getString { key, default ->
                    when (key) {
                        "repository.maxRetries" -> "3"
                        "repository.timeout" -> "5000"
                        else -> default
                    }
                }
                getInt { key, default ->
                    when (key) {
                        "repository.maxRetries" -> 3
                        else -> default
                    }
                }
                getBoolean { key, default ->
                    when (key) {
                        "repository.cacheEnabled" -> true
                        else -> default
                    }
                }
            }

        val userRepository =
            fakeUserRepository {
                configure { config ->
                    RepositoryConfig(
                        maxRetries = config.getInt("repository.maxRetries", 1),
                        timeout = config.getString("repository.timeout", "1000").toLong(),
                        cacheEnabled = config.getBoolean("repository.cacheEnabled", false),
                    )
                }
            }

        // When - Use foundation ConfigService with domain UserRepository
        val repositoryConfig = userRepository.configure(configService)

        // Then
        assertEquals(3, repositoryConfig.maxRetries)
        assertEquals(5000L, repositoryConfig.timeout)
        assertEquals(true, repositoryConfig.cacheEnabled)
    }

    @Test
    fun `GIVEN OrderService fake WHEN using foundation Logger THEN should log order operations`() =
        runTest {
            // Given - Create foundation fake (cross-module import)
            var loggedMessages = mutableListOf<String>()
            val logger =
                fakeLogger {
                    info { message, tag ->
                        loggedMessages.add(message)
                    }
                }

            val orderService =
                fakeOrderService {
                    createOrder { userId, items, loggerParam ->
                        loggerParam.info("Creating order for user: $userId")
                        Result.success(
                            Order(
                                id = "order123",
                                userId = userId,
                                items =
                                    items.map {
                                        OrderItem(
                                            productId = it.productId,
                                            quantity = it.quantity,
                                            price = 10.0,
                                        )
                                    },
                                status = OrderStatus.PENDING,
                                total = items.sumOf { it.quantity * 10.0 },
                            ),
                        )
                    }
                }

            val orderItems =
                listOf(
                    OrderItem("product1", 2, 10.0),
                    OrderItem("product2", 1, 20.0),
                )

            // When
            val result = orderService.createOrder("user123", orderItems, logger)

            // Then
            assertNotNull(result.getOrNull())
            assertEquals("order123", result.getOrNull()?.id)
            assertEquals(1, loggedMessages.size)
            assertEquals("Creating order for user: user123", loggedMessages[0])
        }

    @Test
    fun `GIVEN OrderService fake WHEN syncWithBackend THEN should return NetworkResponse from foundation module`() =
        runTest {
            // Given - Create foundation fake (cross-module import)
            val logger =
                fakeLogger {
                    debug { message, tag -> }
                }

            val mockNetworkResponse =
                NetworkResponse(
                    statusCode = 200,
                    body = "{\"synced\":true}",
                    headers = mapOf("Content-Type" to "application/json"),
                )

            val orderService =
                fakeOrderService {
                    syncWithBackend { loggerParam ->
                        loggerParam.debug("Syncing with backend")
                        Result.success(mockNetworkResponse)
                    }
                }

            // When
            val result = orderService.syncWithBackend(logger)

            // Then
            assertNotNull(result.getOrNull())
            assertEquals(200, result.getOrNull()?.statusCode)
            assertEquals("{\"synced\":true}", result.getOrNull()?.body)
        }

    @Test
    fun `GIVEN AuthenticationService fake WHEN using foundation Logger and ConfigService THEN should handle multiple cross-module dependencies`() =
        runTest {
            // Given - Create multiple foundation fakes (cross-module imports)
            val logger =
                fakeLogger {
                    info { message, tag -> }
                    minLogLevel { LogLevel.INFO }
                }

            val configService =
                fakeConfigService {
                    getString { key, default ->
                        when (key) {
                            "auth.tokenExpiry" -> "3600000"
                            else -> default
                        }
                    }
                }

            val authService =
                fakeAuthenticationService {
                    refreshToken { token, config, loggerParam ->
                        val expiry = config.getString("auth.tokenExpiry", "1800000").toLong()
                        loggerParam.info("Refreshing token with expiry: $expiry")
                        Result.success(
                            AuthToken(
                                value = "new-token-${token.value}",
                                expiresAt = System.currentTimeMillis() + expiry,
                                userId = token.userId,
                            ),
                        )
                    }
                }

            val oldToken = AuthToken("old-token", System.currentTimeMillis(), "user123")

            // When - Use multiple foundation fakes with domain service
            val result = authService.refreshToken(oldToken, configService, logger)

            // Then
            assertNotNull(result.getOrNull())
            assertEquals("new-token-old-token", result.getOrNull()?.value)
            assertEquals("user123", result.getOrNull()?.userId)
        }
}
