// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.features.order

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpmultimodule.core.analytics.Analytics
import com.rsicarelli.fakt.samples.kmpmultimodule.core.logger.Logger
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.ApiClient
import com.rsicarelli.fakt.samples.kmpmultimodule.core.network.ApiResult
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.Cache
import com.rsicarelli.fakt.samples.kmpmultimodule.core.storage.KeyValueStorage

// ============================================================================
// ORDER FEATURE - E-commerce order management
// Dependencies: core/network, core/logger, core/storage, core/analytics
// ============================================================================

/**
 * Domain model for an order.
 */
data class Order(
    val id: String,
    val userId: String,
    val items: List<OrderItem>,
    val status: OrderStatus,
    val totalAmount: Double,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * Domain model for an order item.
 */
data class OrderItem(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
) {
    val totalPrice: Double
        get() = quantity * unitPrice
}

/**
 * Order status enum.
 */
enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    REFUNDED,
}

/**
 * Create order request.
 */
data class CreateOrderRequest(
    val userId: String,
    val items: List<OrderItem>,
    val shippingAddress: String,
    val paymentMethod: String,
)

/**
 * Order use case - Business logic for order operations.
 */
@Fake
interface OrderUseCase {
    /**
     * Create a new order.
     */
    suspend fun createOrder(
        request: CreateOrderRequest,
        apiClient: ApiClient,
        logger: Logger,
        analytics: Analytics,
    ): ApiResult<Order>

    /**
     * Get order by ID.
     */
    suspend fun getOrder(
        orderId: String,
        apiClient: ApiClient,
        cache: Cache,
        logger: Logger,
    ): ApiResult<Order>

    /**
     * Update order status.
     */
    suspend fun updateOrderStatus(
        orderId: String,
        newStatus: OrderStatus,
        apiClient: ApiClient,
        logger: Logger,
        analytics: Analytics,
    ): ApiResult<Order>

    /**
     * Cancel an order.
     */
    suspend fun cancelOrder(
        orderId: String,
        reason: String,
        apiClient: ApiClient,
        logger: Logger,
    ): ApiResult<Unit>

    /**
     * Get user's order history.
     */
    suspend fun getUserOrders(
        userId: String,
        apiClient: ApiClient,
        cache: Cache,
    ): ApiResult<List<Order>>
}

/**
 * Order repository - Data layer for orders.
 */
@Fake
interface OrderRepository {
    /**
     * Fetch order from API.
     */
    suspend fun fetchOrder(orderId: String, apiClient: ApiClient): ApiResult<Order>

    /**
     * Save order to local cache.
     */
    fun saveOrderToCache(order: Order, cache: Cache)

    /**
     * Get order from cache.
     */
    fun getOrderFromCache(orderId: String, cache: Cache): Order?

    /**
     * Save order history to storage.
     */
    suspend fun saveOrderHistory(userId: String, orders: List<Order>, storage: KeyValueStorage)

    /**
     * Get order history from storage.
     */
    suspend fun getOrderHistory(userId: String, storage: KeyValueStorage): List<Order>
}

/**
 * Order validator - Validates order data.
 */
@Fake
interface OrderValidator {
    /**
     * Validate create order request.
     */
    fun validateCreateOrderRequest(request: CreateOrderRequest): OrderValidationResult

    /**
     * Validate order items (quantities, prices, etc).
     */
    fun validateOrderItems(items: List<OrderItem>): OrderValidationResult

    /**
     * Check if order can be cancelled based on current status.
     */
    fun canCancelOrder(order: Order): Boolean
}

/**
 * Order validation result.
 */
sealed class OrderValidationResult {
    data object Valid : OrderValidationResult()

    data class Invalid(val errors: List<String>) : OrderValidationResult()
}
