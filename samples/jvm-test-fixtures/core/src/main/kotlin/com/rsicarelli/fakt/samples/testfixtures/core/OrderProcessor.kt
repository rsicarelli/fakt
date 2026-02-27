// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.testfixtures.core.models.Order
import com.rsicarelli.fakt.samples.testfixtures.core.models.OrderResult
import com.rsicarelli.fakt.samples.testfixtures.core.models.OrderStatus

/**
 * Abstract class for order processing.
 *
 * Demonstrates @Fake on an abstract class with suspend functions.
 * Generated fakes are placed in testFixtures for cross-module consumption.
 */
@Fake
public abstract class OrderProcessor {
    public abstract suspend fun processOrder(order: Order): OrderResult

    public abstract suspend fun cancelOrder(orderId: String): Boolean

    public abstract fun getOrderStatus(orderId: String): OrderStatus
}
