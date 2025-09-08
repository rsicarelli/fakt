// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package core.business

import dev.rsicarelli.ktfake.Fake
import api.shared.NetworkService
import api.shared.StorageService
import api.shared.LoggingService

// ============================================================================
// BUSINESS LOGIC INTERFACES - Core domain interfaces using API dependencies
// Note: Simplified for current compiler capabilities
// ============================================================================

data class User(
    val id: String,
    val name: String,
    val email: String,
    val isActive: Boolean = true
)

data class Order(
    val id: String,
    val userId: String,
    val totalAmount: Double,
    val status: String = "PENDING"
)

enum class OrderStatus {
    PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED
}

@Fake
interface UserService {
    // Cross-module dependency - uses API interfaces
    val networkService: NetworkService
    val storageService: StorageService
    val loggingService: LoggingService

    suspend fun createUser(name: String, email: String): User?
    suspend fun getUserById(id: String): User?
    suspend fun updateUser(user: User): User?
    suspend fun deleteUser(id: String): Boolean
    suspend fun isUserActive(id: String): Boolean
}

@Fake
interface OrderService {
    val userService: UserService
    val networkService: NetworkService
    val storageService: StorageService

    suspend fun createOrder(userId: String, amount: Double): Order?
    suspend fun getOrderById(id: String): Order?
    suspend fun updateOrderStatus(orderId: String, status: String): Order?
    suspend fun cancelOrder(orderId: String): Boolean
    suspend fun calculateTotal(amount: Double, tax: Double): Double
}

@Fake
interface PaymentService {
    suspend fun processPayment(orderId: String, amount: Double): String?
    suspend fun refundPayment(paymentId: String): String?
    suspend fun getPaymentStatus(paymentId: String): String?
    val isProcessing: Boolean
}
