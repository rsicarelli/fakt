// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.genericsBasic
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * P0.0: Class-level generic fake generation ✅
 *
 * Validates that:
 * 1. Generated factory function accepts type parameter (inline fun <reified T>)
 * 2. Type safety is preserved at use-site
 * 3. Configuration DSL works with generic types
 * 4. Different type instantiations maintain separate type safety
 */
class SimpleRepositoryTest {
    data class User(
        val id: String,
        val name: String,
    )

    data class Product(
        val id: String,
        val price: Double,
    )

    @Test
    fun `GIVEN generic repository WHEN configured with User type THEN should maintain type safety`() {
        // Given - create fake with explicit type parameter
        val userRepo =
            fakeSimpleRepository<User> {
                save { user -> user.copy(id = "saved-${user.id}") }
                findAll { listOf(User("1", "Alice"), User("2", "Bob")) }
            }

        // When - use the repository
        val inputUser = User("123", "Test User")
        val savedUser = userRepo.save(inputUser)
        val allUsers = userRepo.findAll()

        // Then - type safety preserved, behavior configured correctly
        assertEquals("saved-123", savedUser.id)
        assertEquals("Test User", savedUser.name)
        assertEquals(2, allUsers.size)
        assertEquals("Alice", allUsers[0].name)
    }

    @Test
    fun `GIVEN generic repository WHEN configured with Product type THEN should maintain type safety`() {
        // Given - create fake with different type parameter
        val productRepo =
            fakeSimpleRepository<Product> {
                save { product -> product.copy(price = product.price * 1.1) }
                findAll { listOf(Product("p1", 99.99), Product("p2", 149.99)) }
            }

        // When
        val inputProduct = Product("new", 100.0)
        val savedProduct = productRepo.save(inputProduct)
        val allProducts = productRepo.findAll()

        // Then - different type, same generic interface
        assertEquals("new", savedProduct.id)
        assertEquals(110.0, savedProduct.price, 0.01)
        assertEquals(2, allProducts.size)
        assertEquals(99.99, allProducts[0].price, 0.01)
    }

    @Test
    fun `GIVEN generic repository WHEN using default behaviors THEN should have sensible defaults`() {
        // Given - create fake without configuration
        val repo = fakeSimpleRepository<String>()

        // When - use default behaviors
        val allItems = repo.findAll()

        // Then - findAll defaults to empty list
        assertEquals(emptyList(), allItems)
    }

    @Test
    fun `GIVEN generic repository WHEN partially configured THEN should use configured and default behaviors`() {
        // Given - configure only save behavior
        val repo =
            fakeSimpleRepository<Int> {
                save { it * 2 }
                // findAll not configured - will use default
            }

        // When
        val saved = repo.save(42)
        val all = repo.findAll()

        // Then
        assertEquals(84, saved)
        assertEquals(emptyList(), all)
    }
}
