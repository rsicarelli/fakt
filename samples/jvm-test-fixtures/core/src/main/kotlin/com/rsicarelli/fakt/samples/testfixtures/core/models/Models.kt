// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core.models

public data class User(
    val id: String,
    val name: String,
    val email: String = "",
)

public data class Order(
    val id: String,
    val userId: String,
    val items: List<String>,
    val total: Double,
)

public enum class OrderStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    CANCELLED,
}

public data class OrderResult(
    val orderId: String,
    val status: OrderStatus,
    val message: String = "",
)
