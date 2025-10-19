// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.order

import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.ApiClient
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.ApiResult
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.Cache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Order screen state.
 */
sealed class OrderState {
    data object Idle : OrderState()
    data object Loading : OrderState()
    data class Success(val orders: List<Order>) : OrderState()
    data object CreatingOrder : OrderState()
    data class OrderCreated(val order: Order) : OrderState()
    data class Error(val message: String) : OrderState()
}

/**
 * Vanilla ViewModel for Order feature (no Android dependencies).
 *
 * Demonstrates production-ready patterns:
 * - StateFlow for reactive state management
 * - K2.2+ backing fields pattern (get() = _field)
 * - Thread-safe state updates with .update { }
 * - Multi-step order creation flow
 * - Order cancellation with confirmation
 * - Retry mechanism for failed operations
 * - Analytics tracking for business events
 * - Coroutine scope for async operations
 *
 * This serves as a real-world example for testing with Fakt + Turbine.
 *
 * NOTE: Call counts are automatically tracked by Fakt fakes!
 * Use `orderUseCase.createOrderCallCount`, `cancelOrderCallCount`, etc. in tests.
 */
class OrderViewModel(
    private val orderUseCase: OrderUseCase,
    private val userId: String,
    private val apiClient: ApiClient,
    private val cache: Cache,
    private val logger: Logger,
    private val analytics: Analytics,
    private val scope: CoroutineScope,
) {
    // State - K2.2+ backing field pattern
    private val _state = MutableStateFlow<OrderState>(OrderState.Idle)
    val state: StateFlow<OrderState>
        get() = _state

    /**
     * Load user's order history.
     * Transitions: Idle -> Loading -> Success/Error
     */
    fun loadOrders() {
        scope.launch {
            try {
                _state.update { OrderState.Loading }

                logger.info("Loading orders for user: $userId")
                val result = orderUseCase.getUserOrders(userId, apiClient, cache)

                when (result) {
                    is ApiResult.Success -> {
                        _state.update { OrderState.Success(result.data) }
                        logger.info("Orders loaded: ${result.data.size} orders")
                        analytics.track("orders_loaded", mapOf("count" to result.data.size.toString()))
                    }
                    is ApiResult.Error -> {
                        _state.update { OrderState.Error(result.message) }
                        logger.error("Failed to load orders", throwable = null, data = mapOf("error" to result.message))
                        analytics.track("orders_load_failed", mapOf("error" to result.message))
                    }
                }
            } catch (e: Exception) {
                _state.update { OrderState.Error(e.message ?: "Unknown error") }
                logger.error("Exception loading orders", e)
                analytics.track("orders_load_exception", mapOf("error" to e.message.orEmpty()))
            }
        }
    }

    /**
     * Create a new order.
     * Multi-step flow: Idle/Success -> CreatingOrder -> OrderCreated/Error
     */
    fun createOrder(request: CreateOrderRequest) {
        scope.launch {
            try {
                _state.update { OrderState.CreatingOrder }

                logger.info("Creating order: ${request.items.size} items, total: ${request.items.sumOf { it.totalPrice }}")
                val result = orderUseCase.createOrder(request, apiClient, logger, analytics)

                when (result) {
                    is ApiResult.Success -> {
                        _state.update { OrderState.OrderCreated(result.data) }
                        logger.info("Order created successfully: ${result.data.id}")
                        analytics.track(
                            "order_created",
                            mapOf(
                                "order_id" to result.data.id,
                                "total_amount" to result.data.totalAmount.toString(),
                                "items_count" to result.data.items.size.toString(),
                            ),
                        )
                    }
                    is ApiResult.Error -> {
                        _state.update { OrderState.Error(result.message) }
                        logger.error("Failed to create order", throwable = null, data = mapOf("error" to result.message))
                        analytics.track("order_creation_failed", mapOf("error" to result.message))
                    }
                }
            } catch (e: Exception) {
                _state.update { OrderState.Error(e.message ?: "Unknown error") }
                logger.error("Exception creating order", e)
                analytics.track("order_creation_exception", mapOf("error" to e.message.orEmpty()))
            }
        }
    }

    /**
     * Cancel an order.
     * Requires confirmation before calling the API.
     */
    fun cancelOrder(orderId: String, reason: String) {
        scope.launch {
            try {
                logger.warn("Cancelling order: $orderId, reason: $reason")

                val result = orderUseCase.cancelOrder(orderId, reason, apiClient, logger)

                when (result) {
                    is ApiResult.Success -> {
                        // Reload orders to reflect the cancellation
                        loadOrders()
                        logger.info("Order cancelled successfully: $orderId")
                        analytics.track(
                            "order_cancelled",
                            mapOf(
                                "order_id" to orderId,
                                "reason" to reason,
                            ),
                        )
                    }
                    is ApiResult.Error -> {
                        _state.update { OrderState.Error(result.message) }
                        logger.error("Failed to cancel order", throwable = null, data = mapOf("error" to result.message))
                        analytics.track("order_cancellation_failed", mapOf("error" to result.message))
                    }
                }
            } catch (e: Exception) {
                _state.update { OrderState.Error(e.message ?: "Unknown error") }
                logger.error("Exception cancelling order", e)
                analytics.track("order_cancellation_exception", mapOf("error" to e.message.orEmpty()))
            }
        }
    }

    /**
     * Get a single order by ID.
     */
    fun getOrder(orderId: String) {
        scope.launch {
            try {
                _state.update { OrderState.Loading }

                logger.info("Loading order: $orderId")
                val result = orderUseCase.getOrder(orderId, apiClient, cache, logger)

                when (result) {
                    is ApiResult.Success -> {
                        _state.update { OrderState.Success(listOf(result.data)) }
                        logger.info("Order loaded: ${result.data.id}")
                    }
                    is ApiResult.Error -> {
                        _state.update { OrderState.Error(result.message) }
                        logger.error("Failed to load order", throwable = null, data = mapOf("error" to result.message))
                    }
                }
            } catch (e: Exception) {
                _state.update { OrderState.Error(e.message ?: "Unknown error") }
                logger.error("Exception loading order", e)
            }
        }
    }

    /**
     * Retry after an error.
     * Returns to Idle state to allow user to try again.
     */
    fun retry() {
        val currentState = _state.value
        if (currentState is OrderState.Error) {
            logger.info("Retrying after error")
            _state.update { OrderState.Idle }
        }
    }

    /**
     * Reset to Idle state (e.g., after order created, return to order list).
     */
    fun resetToIdle() {
        _state.update { OrderState.Idle }
    }
}
