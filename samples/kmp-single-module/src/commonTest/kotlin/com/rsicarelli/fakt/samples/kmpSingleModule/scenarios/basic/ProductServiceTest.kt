// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.basic

import com.rsicarelli.fakt.samples.kmpSingleModule.models.Product
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Comprehensive test for ProductService fake generation.
 */
class ProductServiceTest {
    @Test
    fun `GIVEN ProductService fake WHEN calling suspend getProduct THEN should return Product`() =
        runTest {
            // Given
            val fake = fakeProductService {
                getProduct { id ->
                    if (id == 1L) Product(1L, "Test Product", 99.99, "Electronics") else null
                }
            }

            // When
            val foundProduct = fake.getProduct(1L)
            val missingProduct = fake.getProduct(999L)

            // Then
            assertEquals("Test Product", foundProduct?.name)
            assertEquals(99.99, foundProduct?.price)
            assertNull(missingProduct)
        }

    @Test
    fun `GIVEN ProductService fake WHEN searching with defaults THEN should handle default limit`() =
        runTest {
            // Given
            val fake = fakeProductService {
                searchProducts { query, limit ->
                    List(limit) { index ->
                        Product(index.toLong(), "$query-product-$index", 10.0 * index, "Test")
                    }
                }
            }

            // When - call with explicit limit
            val resultWithLimit = fake.searchProducts("test", 5)
            // When - call with default limit (10)
            val resultDefaultLimit = fake.searchProducts("default")

            // Then
            assertEquals(5, resultWithLimit.size)
            assertEquals("test-product-0", resultWithLimit[0].name)
            assertEquals(10, resultDefaultLimit.size)
            assertEquals("default-product-9", resultDefaultLimit[9].name)
        }

    @Test
    fun `GIVEN ProductService fake WHEN updating price THEN should return updated product`() =
        runTest {
            // Given
            val fake = fakeProductService {
                updatePrice { id, newPrice ->
                    Product(id, "Updated Product", newPrice, "Updated")
                }
            }

            // When
            val result = fake.updatePrice(123L, 149.99)

            // Then
            assertEquals(123L, result.id)
            assertEquals("Updated Product", result.name)
            assertEquals(149.99, result.price)
        }

    @Test
    fun `GIVEN ProductService fake WHEN using defaults THEN should have sensible async defaults`() =
        runTest {
            // Given
            val fake = fakeProductService()

            // When
            val product = fake.getProduct(1L)
            val searchResults = fake.searchProducts("query", 5)

            // Then
            assertNull(product) // Default: null
            assertEquals(0, searchResults.size) // Default: empty list
            // Note: updatePrice returns Product (domain type) - requires configuration
        }
}
