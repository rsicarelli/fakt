// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.basic

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.singlemodule.models.Product

/**
 * Suspend-only interface with domain types and default parameters.
 *
 * Tests async operations pattern with:
 * - All methods are suspend functions
 * - Custom domain type (Product)
 * - Default parameter (limit = 10 in searchProducts)
 * - Nullable return types
 * Validates coroutine-based service layer patterns.
 */
@Fake
interface ProductService {
    suspend fun getProduct(id: Long): Product?

    suspend fun searchProducts(
        query: String,
        limit: Int = 10,
    ): List<Product>

    suspend fun updatePrice(
        id: Long,
        newPrice: Double,
    ): Product
}
