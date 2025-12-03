// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch009

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpBenchmark.models.Product

@Fake
interface ProductService_basic853 {
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
