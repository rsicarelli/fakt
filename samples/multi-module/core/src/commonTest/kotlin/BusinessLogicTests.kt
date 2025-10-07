// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package core.business

import api.shared.LoggingService
import api.shared.NetworkService
import api.shared.StorageService
import api.shared.fakeLoggingService
import api.shared.fakeNetworkService
import api.shared.fakeStorageService
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
        // Create cross-module dependency fakes
        val fakeNetworkService = fakeNetworkService()
        val fakeStorageService = fakeStorageService()
        val fakeLoggingService = fakeLoggingService()

        val fakeUserService =
            fakeUserService {
                // Configure cross-module dependencies
                networkService { fakeNetworkService }
                storageService { fakeStorageService }
                loggingService { fakeLoggingService }
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
        // Create nested dependency fakes
        val fakeNetworkService = fakeNetworkService()
        val fakeStorageService = fakeStorageService()
        val fakeLoggingService = fakeLoggingService()

        val fakeUserService =
            fakeUserService {
                networkService { fakeNetworkService }
                storageService { fakeStorageService }
                loggingService { fakeLoggingService }
            }

        val fakeOrderService =
            fakeOrderService {
                userService { fakeUserService }
                networkService { fakeNetworkService }
                storageService { fakeStorageService }
            }

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
        val fakeUserService =
            fakeUserService {
                // In the future, this will configure behavior like:
                // networkService {
                //     get { url -> Result.success("mock-response") }
                // }
            }

        val fakeOrderService =
            fakeOrderService {
                // Configure with the fake user service
                // userService { fakeUserService }
            }

        assertNotNull(fakeUserService)
        assertNotNull(fakeOrderService)

        // This demonstrates how complex business logic can be tested
        // with properly configured fakes across module boundaries
    }
}
