// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.order

import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.fakeAnalytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.fakeLogger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.ApiResult
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.fakeApiClient
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.fakeCache
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

// Test timestamp constants (KMP-compatible)
private const val TEST_TIMESTAMP = 1_735_689_600_000L // 2025-01-01 00:00:00 UTC

class OrderUseCaseTest {

    @Test
    fun `GIVEN OrderUseCase WHEN creating order THEN should return created order`() =
        runTest {
            // Given
            val mockOrder =
                Order(
                    id = "order-123",
                    userId = "user-456",
                    items =
                        listOf(
                            OrderItem("prod-1", "Product 1", 2, 19.99),
                        ),
                    status = OrderStatus.PENDING,
                    totalAmount = 39.98,
                    createdAt = TEST_TIMESTAMP,
                    updatedAt = TEST_TIMESTAMP,
                )

            val apiClient =
                fakeApiClient {
                    post<CreateOrderRequest, Order> { endpoint, body, _ ->
                        if (endpoint == "/orders") {
                            ApiResult.Success(mockOrder)
                        } else {
                            ApiResult.Error(404, "Not found")
                        }
                    }
                }

            val trackedEvents = mutableListOf<String>()
            val analytics =
                fakeAnalytics {
                    track { eventName, _ ->
                        trackedEvents.add(eventName)
                    }
                }

            val logger = fakeLogger()

            val useCase =
                fakeOrderUseCase {
                    createOrder { request, api, log, track ->
                        log.info("Creating order for user: ${request.userId}")
                        val result: ApiResult<Order> = api.post("/orders", request)
                        if (result is ApiResult.Success) {
                            track.track("order_created")
                        }
                        result
                    }
                }

            // When
            val request = CreateOrderRequest("user-456", listOf(OrderItem("prod-1", "Product 1", 2, 19.99)), "123 Main St", "credit_card")
            val result = useCase.createOrder(request, apiClient, logger, analytics)

            // Then
            assertTrue(result is ApiResult.Success)
            assertEquals("order-123", result.data.id)
            assertTrue(trackedEvents.contains("order_created"))
        }

    @Test
    fun `GIVEN OrderUseCase WHEN getting order with cache THEN should use cached order`() =
        runTest {
            // Given
            val cachedOrder =
                Order(
                    id = "order-123",
                    userId = "user-456",
                    items = emptyList(),
                    status = OrderStatus.DELIVERED,
                    totalAmount = 99.99,
                    createdAt = TEST_TIMESTAMP,
                    updatedAt = TEST_TIMESTAMP,
                )

            val cacheData = mutableMapOf<String, Any>()
            val cache =
                fakeCache {
                    get { key ->
                        cacheData[key]
                    }
                    put { key, value, _ ->
                        cacheData[key] = value
                    }
                }

            // Pre-populate cache
            cacheData["order_order-123"] = cachedOrder

            val apiClient = fakeApiClient()
            val logger = fakeLogger()

            val useCase =
                fakeOrderUseCase {
                    getOrder { orderId, api, cacheStore, _ ->
                        val cached = cacheStore.get("order_$orderId") as? Order
                        if (cached != null) {
                            ApiResult.Success(cached)
                        } else {
                            api.get("/orders/$orderId")
                        }
                    }
                }

            // When
            val result = useCase.getOrder("order-123", apiClient, cache, logger)

            // Then
            assertTrue(result is ApiResult.Success)
            assertEquals(OrderStatus.DELIVERED, result.data.status)
        }
}

class OrderValidatorTest {

    @Test
    fun `GIVEN OrderValidator WHEN validating valid request THEN should return Valid`() {
        // Given
        val validator =
            fakeOrderValidator {
                validateCreateOrderRequest { request ->
                    if (request.items.isNotEmpty() && request.userId.isNotBlank()) {
                        OrderValidationResult.Valid
                    } else {
                        OrderValidationResult.Invalid(listOf("Invalid order request"))
                    }
                }
            }

        // When
        val request = CreateOrderRequest("user-123", listOf(OrderItem("prod-1", "Product", 1, 10.0)), "Address", "card")
        val result = validator.validateCreateOrderRequest(request)

        // Then
        assertTrue(result is OrderValidationResult.Valid)
    }
}
