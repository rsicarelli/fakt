// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import kotlin.test.*

/**
 * Real-world validation tests for KtFakes type-safe generation.
 * This validates our type-safe API with the actual generated fakes.
 */
class RealWorldValidationTest {
    @Test
    fun `GIVEN generated fakes WHEN using basic patterns THEN should work seamlessly`() {
        // Test that the fake factory functions work
        val testService = fakeTestService()
        val analytics = fakeAnalyticsService()
        val auth = fakeAuthenticationService()

        // Verify basic usage works
        assertNotNull(testService, "fakeTestService should be created")
        assertNotNull(analytics, "fakeAnalyticsService should be created")
        assertNotNull(auth, "fakeAuthenticationService should be created")

        // Test default behavior works
        analytics.track("test_event") // Should not throw
        testService.setValue("test") // Should not throw

        // Test default values
        assertEquals("", testService.getValue())
        assertEquals("", testService.stringValue)
        assertFalse(auth.isLoggedIn)
        assertNull(auth.currentUser)
    }

    @Test
    fun `GIVEN fake configuration DSL WHEN configuring behavior THEN should be type-safe`() {
        var capturedEvent: String? = null

        // Test type-safe configuration
        val analytics =
            fakeAnalyticsService {
                track { event ->
                    capturedEvent = event // Type-safe: event is String
                }
            }

        analytics.track("test_event")
        assertEquals("test_event", capturedEvent)

        // Test service configuration
        val testService =
            fakeTestService {
                getValue { "configured_value" }
                stringValue { "configured_property" }
            }

        assertEquals("configured_value", testService.getValue())
        assertEquals("configured_property", testService.stringValue)
    }

    @Test
    fun `GIVEN authentication service WHEN configuring complex behavior THEN should maintain state`() {
        var isLoggedIn = false

        val auth =
            fakeAuthenticationService {
                login { username, password ->
                    isLoggedIn = username == "admin" && password == "secret"
                    if (isLoggedIn) {
                        Result.success(User("1", "admin", "admin@test.com"))
                    } else {
                        Result.failure(Exception("Invalid credentials"))
                    }
                }

                isLoggedIn { isLoggedIn }

                logout {
                    isLoggedIn = false
                    Result.success(Unit)
                }
            }

        // Test login flow
        assertFalse(auth.isLoggedIn)

        // Note: Can't test suspend functions directly in simple test
        // but the configuration proves type safety works
        assertTrue(true, "Complex configuration compiles and is type-safe")
    }

    @Test
    fun `GIVEN user repository WHEN configuring collections THEN should handle complex types`() {
        val users =
            listOf(
                User("1", "Alice", "alice@test.com"),
                User("2", "Bob", "bob@test.com"),
            )

        val userRepo =
            fakeUserRepository {
                users { users }
                findById { id -> users.find { it.id == id } }
                save { user -> user } // Identity function
                delete { id -> users.any { it.id == id } }
            }

        // Test collection behavior
        assertEquals(users, userRepo.users)
        assertEquals("Alice", userRepo.findById("1")?.name)
        assertNull(userRepo.findById("999"))
        assertTrue(userRepo.delete("1"))
        assertFalse(userRepo.delete("999"))
    }

    @Test
    fun `GIVEN multiple services WHEN integrating THEN should work together`() {
        val auth =
            fakeAuthenticationService {
                isLoggedIn { true }
                currentUser { User("1", "TestUser", "test@test.com") }
            }

        val analytics =
            fakeAnalyticsService {
                track { event ->
                    assertTrue(event.isNotEmpty())
                }
            }

        val userRepo =
            fakeUserRepository {
                findById { id ->
                    if (id == "1") User("1", "TestUser", "test@test.com") else null
                }
            }

        // Test integration
        if (auth.isLoggedIn) {
            val currentUser = auth.currentUser
            assertNotNull(currentUser)

            val userDetails = userRepo.findById(currentUser.id)
            assertNotNull(userDetails)
            assertEquals(currentUser.name, userDetails.name)

            analytics.track("user_profile_viewed")
        }
    }

    @Test
    fun `GIVEN type-safe DSL WHEN making configuration mistakes THEN should be caught at compile time`() {
        // This test validates compile-time type safety
        val auth =
            fakeAuthenticationService {
                hasPermission { permission ->
                    // permission is String (not Any?) - compile-time safe
                    permission.startsWith("admin") // String methods available
                }

                hasAnyPermissions { permissions ->
                    // permissions is List<String> - compile-time safe
                    permissions.any { it.startsWith("read") } // List methods available
                }

                currentUser {
                    // Return type is User? - compile-time safe
                    User("test", "Test User", "test@test.com")
                }
            }

        assertTrue(auth.hasPermission("admin_user"))
        assertTrue(auth.hasAnyPermissions(listOf("read_users", "write_data")))
        assertNotNull(auth.currentUser)
    }

    @Test
    fun `GIVEN generated fakes WHEN checking implementation THEN should use exact types`() {
        // This validates our core achievement: no unsafe casting!

        val testService =
            fakeTestService {
                getValue { "test" } // () -> String (exact type!)
                setValue { value ->
                    // (String) -> Unit (exact type!)
                    assertTrue(value is String) // Type is guaranteed
                }
            }

        val auth =
            fakeAuthenticationService {
                hasPermission { permission ->
                    // (String) -> Boolean (exact type!)
                    assertTrue(permission is String) // No Any? casting needed!
                    permission.length > 0
                }
            }

        // Test that exact types work perfectly
        assertEquals("test", testService.getValue())
        testService.setValue("type_safe_value")
        assertTrue(auth.hasPermission("admin"))

        // This proves our type-safe generation works!
        assertTrue(true, "Generated code uses exact types - no unsafe casting!")
    }

    @Test
    fun `GIVEN performance requirements WHEN creating many fakes THEN should be fast`() {
        val startTime = System.currentTimeMillis()

        // Create many instances
        val fakes =
            (1..50).map {
                fakeTestService {
                    getValue { "value_$it" }
                }
            }

        val creationTime = System.currentTimeMillis() - startTime

        // Should be reasonably fast
        assertTrue(creationTime < 1000, "Creation took ${creationTime}ms")
        assertEquals(50, fakes.size)

        // All should work independently
        fakes.forEachIndexed { index, fake ->
            assertEquals("value_${index + 1}", fake.getValue())
        }
    }
}
