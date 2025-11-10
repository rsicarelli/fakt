// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.rsicarelli.fakt.samples.kmpmultimodule.features.order

import app.cash.turbine.test
import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.fakeAnalytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.fakeLogger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.ApiClient
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.ApiResult
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.fakeApiClient
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.Cache
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.fakeCache
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OrderViewModelTest {

    companion object {
        // Helper to create test order
        private fun createTestOrder(
            id: String = "order-1",
            userId: String = "user-123",
            status: OrderStatus = OrderStatus.PENDING,
            totalAmount: Double = 100.0,
        ) = Order(
            id = id,
            userId = userId,
            items =
                listOf(
                    OrderItem(
                        productId = "prod-1",
                        productName = "Test Product",
                        quantity = 1,
                        unitPrice = totalAmount,
                    ),
                ),
            status = status,
            totalAmount = totalAmount,
            createdAt = 1234567890L,
            updatedAt = 1234567890L,
        )

        // Helper to create test order request
        private fun createTestOrderRequest(
            userId: String = "user-123",
            items: List<OrderItem> =
                listOf(
                    OrderItem("prod-1", "Test Product", 1, 100.0),
                ),
        ) = CreateOrderRequest(
            userId = userId,
            items = items,
            shippingAddress = "123 Test St",
            paymentMethod = "credit_card",
        )
    }

    // ============================================================================
    // LOAD ORDERS TESTS
    // ============================================================================

    @Test
    fun `GIVEN valid userId WHEN loading orders THEN should transition to Success`() =
        runTest {
            // Given
            val orders = listOf(createTestOrder(id = "1"), createTestOrder(id = "2"))

            val orderUseCase =
                fakeOrderUseCase {
                    getUserOrders { _, _, _ -> ApiResult.Success(orders) }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.state.test {
                assertEquals(OrderState.Idle, awaitItem())

                viewModel.loadOrders()
                advanceUntilIdle()

                assertEquals(OrderState.Loading, awaitItem())
                val successState = awaitItem()
                assertTrue(successState is OrderState.Success)
                assertEquals(2, successState.orders.size)
            }
        }

    @Test
    fun `GIVEN empty order history WHEN loading orders THEN should show Success with empty list`() =
        runTest {
            // Given
            val orderUseCase =
                fakeOrderUseCase {
                    getUserOrders { _, _, _ -> ApiResult.Success(emptyList()) }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.loadOrders()
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is OrderState.Success)
                assertEquals(0, state.orders.size)
            }
        }

    @Test
    fun `GIVEN API failure WHEN loading orders THEN should show Error state`() =
        runTest {
            // Given
            val orderUseCase =
                fakeOrderUseCase {
                    getUserOrders { _, _, _ -> ApiResult.Error(500, "Network error") }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.loadOrders()
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is OrderState.Error)
                assertEquals("Network error", state.message)
            }
        }

    // ============================================================================
    // CREATE ORDER TESTS
    // ============================================================================

    @Test
    fun `GIVEN valid order request WHEN creating order THEN should transition to OrderCreated`() =
        runTest {
            // Given
            val request = createTestOrderRequest()
            val createdOrder = createTestOrder()

            val orderUseCase =
                fakeOrderUseCase {
                    createOrder { _, _, _, _ -> ApiResult.Success(createdOrder) }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.state.test {
                assertEquals(OrderState.Idle, awaitItem())

                viewModel.createOrder(request)
                advanceUntilIdle()

                assertEquals(OrderState.CreatingOrder, awaitItem())
                val orderCreatedState = awaitItem()
                assertTrue(orderCreatedState is OrderState.OrderCreated)
                assertEquals(createdOrder.id, orderCreatedState.order.id)
            }
        }

    @Test
    fun `GIVEN API failure WHEN creating order THEN should show Error state`() =
        runTest {
            // Given
            val request = createTestOrderRequest()

            val orderUseCase =
                fakeOrderUseCase {
                    createOrder { _, _, _, _ -> ApiResult.Error(400, "Payment failed") }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.createOrder(request)
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is OrderState.Error)
                assertEquals("Payment failed", state.message)
            }
        }

    @Test
    fun `GIVEN order creation exception WHEN creating order THEN should handle gracefully`() =
        runTest {
            // Given
            val request = createTestOrderRequest()

            val orderUseCase =
                fakeOrderUseCase {
                    createOrder { _, _, _, _ ->
                        throw RuntimeException("Validation failed")
                    }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.createOrder(request)
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is OrderState.Error)
                assertEquals("Validation failed", state.message)
            }
        }

    // ============================================================================
    // CANCEL ORDER TESTS
    // ============================================================================

    @Test
    fun `GIVEN existing order WHEN cancelling order THEN should reload orders`() =
        runTest {
            // Given
            val orders = listOf(createTestOrder(id = "1", status = OrderStatus.PENDING))

            val orderUseCase =
                fakeOrderUseCase {
                    cancelOrder { _, _, _, _ -> ApiResult.Success(Unit) }
                    getUserOrders { _, _, _ -> ApiResult.Success(orders) }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.state.test {
                assertEquals(OrderState.Idle, awaitItem())

                viewModel.cancelOrder("1", "Changed mind")
                advanceUntilIdle()

                // Should reload orders after cancellation
                assertEquals(OrderState.Loading, awaitItem())
                val successState = awaitItem()
                assertTrue(successState is OrderState.Success)
            }
        }

    @Test
    fun `GIVEN API failure WHEN cancelling order THEN should show Error state`() =
        runTest {
            // Given
            val orderUseCase =
                fakeOrderUseCase {
                    cancelOrder { _, _, _, _ -> ApiResult.Error(400, "Cannot cancel shipped order") }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.cancelOrder("1", "Changed mind")
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is OrderState.Error)
                assertEquals("Cannot cancel shipped order", state.message)
            }
        }

    // ============================================================================
    // GET SINGLE ORDER TESTS
    // ============================================================================

    @Test
    fun `GIVEN valid orderId WHEN getting order THEN should show Success with single order`() =
        runTest {
            // Given
            val order = createTestOrder(id = "order-123")

            val orderUseCase =
                fakeOrderUseCase {
                    getOrder { _, _, _, _ -> ApiResult.Success(order) }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.getOrder("order-123")
            advanceUntilIdle()

            // Then
            viewModel.state.test {
                val state = awaitItem()
                assertTrue(state is OrderState.Success)
                assertEquals(1, state.orders.size)
                assertEquals("order-123", state.orders[0].id)
            }
        }

    // ============================================================================
    // RETRY LOGIC TESTS
    // ============================================================================

    @Test
    fun `GIVEN Error state WHEN retrying THEN should reset to Idle`() =
        runTest {
            // Given
            val orderUseCase =
                fakeOrderUseCase {
                    getUserOrders { _, _, _ -> ApiResult.Error(500, "Network error") }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // Load orders (will fail)
            viewModel.loadOrders()
            advanceUntilIdle()

            // When - Retry
            viewModel.state.test {
                assertTrue(awaitItem() is OrderState.Error)

                viewModel.retry()
                advanceUntilIdle()

                assertEquals(OrderState.Idle, awaitItem())
            }
        }

    // ============================================================================
    // CONCURRENCY TESTS
    // ============================================================================

    @Test
    fun `GIVEN OrderViewModel WHEN 10 concurrent order creations THEN should be thread safe`() =
        runTest {
            // Given
            val orderUseCase =
                fakeOrderUseCase {
                    createOrder { _, _, _, _ ->
                        delay(10)
                        ApiResult.Success(createTestOrder())
                    }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When - 10 concurrent order creations
            repeat(10) { index ->
                launch {
                    viewModel.createOrder(createTestOrderRequest())
                }
            }
            advanceUntilIdle()

            // Then - Fakt tracks all 10 calls!
            orderUseCase.createOrderCallCount.test {
                assertEquals(10, awaitItem())
            }
        }

    // ============================================================================
    // FAKT CALL COUNT TESTS
    // ============================================================================

    @Test
    fun `GIVEN OrderViewModel WHEN loading orders THEN should track getUserOrders call count`() =
        runTest {
            // Given
            val orderUseCase =
                fakeOrderUseCase {
                    getUserOrders { _, _, _ -> ApiResult.Success(listOf(createTestOrder())) }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.loadOrders()
            advanceUntilIdle()

            // Then - Fakt tracks the call automatically!
            orderUseCase.getUserOrdersCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    @Test
    fun `GIVEN OrderViewModel WHEN creating order THEN should track createOrder call count`() =
        runTest {
            // Given
            val orderUseCase =
                fakeOrderUseCase {
                    createOrder { _, _, _, _ -> ApiResult.Success(createTestOrder()) }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.createOrder(createTestOrderRequest())
            advanceUntilIdle()

            // Then - Fakt tracks it!
            orderUseCase.createOrderCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    @Test
    fun `GIVEN OrderViewModel WHEN cancelling order THEN should track cancelOrder call count`() =
        runTest {
            // Given
            val orderUseCase =
                fakeOrderUseCase {
                    cancelOrder { _, _, _, _ -> ApiResult.Success(Unit) }
                    getUserOrders { _, _, _ -> ApiResult.Success(emptyList()) }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase)

            // When
            viewModel.cancelOrder("order-123", "Changed mind")
            advanceUntilIdle()

            // Then - Fakt tracks it!
            orderUseCase.cancelOrderCallCount.test {
                assertEquals(1, awaitItem())
            }
        }

    // ============================================================================
    // ANALYTICS VALIDATION TESTS
    // ============================================================================

    @Test
    fun `GIVEN successful order creation WHEN tracking analytics THEN should track order_created event`() =
        runTest {
            // Given
            val trackedEvents = mutableListOf<String>()
            val analytics =
                fakeAnalytics {
                    track { eventName, _ ->
                        trackedEvents.add(eventName)
                    }
                }

            val orderUseCase =
                fakeOrderUseCase {
                    createOrder { _, _, _, _ -> ApiResult.Success(createTestOrder()) }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase, analytics = analytics)

            // When
            viewModel.createOrder(createTestOrderRequest())
            advanceUntilIdle()

            // Then
            assertTrue(trackedEvents.contains("order_created"))
        }

    @Test
    fun `GIVEN successful order cancellation WHEN tracking analytics THEN should track order_cancelled event`() =
        runTest {
            // Given
            val trackedEvents = mutableListOf<String>()
            val analytics =
                fakeAnalytics {
                    track { eventName, _ ->
                        trackedEvents.add(eventName)
                    }
                }

            val orderUseCase =
                fakeOrderUseCase {
                    cancelOrder { _, _, _, _ -> ApiResult.Success(Unit) }
                    getUserOrders { _, _, _ -> ApiResult.Success(emptyList()) }
                }

            val viewModel = factoryOrderViewModel(orderUseCase = orderUseCase, analytics = analytics)

            // When
            viewModel.cancelOrder("order-123", "Changed mind")
            advanceUntilIdle()

            // Then
            assertTrue(trackedEvents.contains("order_cancelled"))
        }

    // ============================================================================
    // HELPER FACTORY
    // ============================================================================

    private fun TestScope.factoryOrderViewModel(
        orderUseCase: OrderUseCase = fakeOrderUseCase(),
        userId: String = "test-user-123",
        apiClient: ApiClient = fakeApiClient(),
        cache: Cache = fakeCache(),
        logger: Logger = fakeLogger(),
        analytics: Analytics = fakeAnalytics(),
    ) = OrderViewModel(
        orderUseCase = orderUseCase,
        userId = userId,
        apiClient = apiClient,
        cache = cache,
        logger = logger,
        analytics = analytics,
        scope = this,
    )
}
