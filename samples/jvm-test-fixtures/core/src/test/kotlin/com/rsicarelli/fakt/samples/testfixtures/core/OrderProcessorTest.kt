// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core

import com.rsicarelli.fakt.samples.testfixtures.core.models.Order
import com.rsicarelli.fakt.samples.testfixtures.core.models.OrderResult
import com.rsicarelli.fakt.samples.testfixtures.core.models.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderProcessorTest {
    @Test
    fun `GIVEN OrderProcessor fake WHEN processing order THEN returns configured result`() =
        runTest {
            val fake = fakeOrderProcessor {
                processOrder { order ->
                    OrderResult(order.id, OrderStatus.COMPLETED, "Processed")
                }
            }

            val order = Order("order-1", "user-1", listOf("item-a"), 99.99)
            val result = fake.processOrder(order)

            assertEquals("order-1", result.orderId)
            assertEquals(OrderStatus.COMPLETED, result.status)
        }

    @Test
    fun `GIVEN OrderProcessor fake WHEN cancelling order THEN returns success`() = runTest {
        val fake = fakeOrderProcessor {
            cancelOrder { true }
        }

        assertTrue(fake.cancelOrder("order-1"))
    }

    @Test
    fun `GIVEN OrderProcessor fake WHEN getting status THEN returns configured status`() {
        val fake = fakeOrderProcessor {
            getOrderStatus { OrderStatus.PROCESSING }
        }

        assertEquals(OrderStatus.PROCESSING, fake.getOrderStatus("order-1"))
    }
}
