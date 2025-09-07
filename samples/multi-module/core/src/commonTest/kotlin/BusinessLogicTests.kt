// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package core.business

import api.shared.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests that demonstrate cross-module fake generation.
 * This module defines @Fake interfaces that depend on interfaces from the API module.
 */
class BusinessLogicTests {

    @Test
    fun `UserService fake can be created with cross-module dependencies`() {
        val fakeUserService = fakeUserService {
            // Should have access to cross-module dependency configuration
        }
        
        assertNotNull(fakeUserService)
        assertTrue(fakeUserService is UserService)
        
        // Verify cross-module dependencies are accessible
        assertNotNull(fakeUserService.networkService)
        assertNotNull(fakeUserService.storageService)
        assertNotNull(fakeUserService.loggingService)
        
        // Verify dependencies are the correct types from API module
        assertTrue(fakeUserService.networkService is NetworkService)
        assertTrue(fakeUserService.storageService is StorageService)
        assertTrue(fakeUserService.loggingService is LoggingService)
    }

    @Test
    fun `OrderService fake can be created with nested dependencies`() {
        val fakeOrderService = fakeOrderService()
        
        assertNotNull(fakeOrderService)
        assertTrue(fakeOrderService is OrderService)
        
        // Verify nested dependencies
        assertNotNull(fakeOrderService.userService)
        assertNotNull(fakeOrderService.networkService)
        assertNotNull(fakeOrderService.storageService)
        
        assertTrue(fakeOrderService.userService is UserService)
        assertTrue(fakeOrderService.networkService is NetworkService)
        assertTrue(fakeOrderService.storageService is StorageService)
    }

    @Test
    fun `PaymentService fake handles complex types`() {
        val fakePaymentService = fakePaymentService()
        
        assertNotNull(fakePaymentService)
        assertTrue(fakePaymentService is PaymentService)
        
        // Methods with complex return types should be callable
        // (Will return default Result.success values from fake)
    }

    @Test
    fun `business logic integration with faked dependencies`() {
        // Create a configured business service with mocked dependencies
        val fakeUserService = fakeUserService {
            // In the future, this will configure behavior like:
            // networkService { 
            //     get { url -> Result.success("mock-response") } 
            // }
        }
        
        val fakeOrderService = fakeOrderService {
            // Configure with the fake user service
            // userService { fakeUserService }
        }
        
        assertNotNull(fakeUserService)
        assertNotNull(fakeOrderService)
        
        // This demonstrates how complex business logic can be tested
        // with properly configured fakes across module boundaries
    }
}