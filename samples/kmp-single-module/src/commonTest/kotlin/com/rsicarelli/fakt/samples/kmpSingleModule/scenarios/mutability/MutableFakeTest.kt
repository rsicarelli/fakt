// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.mutability

import com.rsicarelli.fakt.samples.kmpSingleModule.models.User
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for mutable fakes feature.
 *
 * Tests the three MutabilityMode options:
 * - MUTABLE: Behaviors can be reconfigured mid-test via configure {}
 * - IMMUTABLE: Behaviors are fixed at construction time (standard)
 * - DEFAULT: Follows plugin configuration (immutable by default)
 */
class MutableFakeTest {
    // ==========================================
    // MutabilityMode.MUTABLE Tests
    // ==========================================

    @Test
    fun `GIVEN mutable fake WHEN configured THEN initial behavior works`() {
        // GIVEN: A mutable repository with initial behavior
        val fake = fakeMutableRepository {
            findById { id -> User(id = id, name = "User $id") }
            status { "active" }
        }

        // WHEN: Using the fake with initial config
        val user = fake.findById("123")

        // THEN: Initial behavior is correct
        assertEquals("123", user?.id)
        assertEquals("User 123", user?.name)
        assertEquals("active", fake.status)
    }

    @Test
    fun `GIVEN mutable fake WHEN reconfigured mid-test THEN new behavior applies`() {
        // GIVEN: A mutable repository
        val fake = fakeMutableRepository {
            findById { id -> User(id = id, name = "User $id") }
            delete { true }
            status { "active" }
        }

        // Verify initial behavior
        assertEquals("User 1", fake.findById("1")?.name)
        assertTrue(fake.delete("1"))

        // WHEN: Reconfiguring the fake mid-test
        fake.configure {
            findById { null } // Now returns null (simulating "not found")
        }

        // THEN: New behavior applies for reconfigured method
        assertNull(fake.findById("1"))
        // Non-reconfigured behaviors remain unchanged
        assertTrue(fake.delete("1"))
        assertEquals("active", fake.status)
    }

    @Test
    fun `GIVEN mutable fake WHEN simulating failure after success THEN throws on second call`() {
        // GIVEN: A mutable repository that initially succeeds
        val fake = fakeMutableRepository {
            delete { true }
            status { "ok" }
        }

        // First call succeeds
        assertTrue(fake.delete("item-1"))

        // WHEN: Reconfiguring to simulate failure
        fake.configure {
            delete { throw IllegalStateException("Database unavailable") }
        }

        // THEN: Second call fails
        assertFailsWith<IllegalStateException> {
            fake.delete("item-2")
        }
    }

    @Test
    fun `GIVEN mutable fake with suspend function WHEN reconfigured THEN suspend behavior changes`() =
        runTest {
            // GIVEN: A mutable repository with suspend save
            val savedUser = User(id = "1", name = "Original")
            val fake = fakeMutableRepository {
                save { savedUser }
                status { "ok" }
            }

            // Verify initial behavior
            assertEquals("Original", fake.save(User(id = "1", name = "Test")).name)

            // WHEN: Reconfiguring the suspend method
            val updatedUser = User(id = "1", name = "Updated")
            fake.configure {
                save { updatedUser }
            }

            // THEN: New suspend behavior applies
            assertEquals("Updated", fake.save(User(id = "1", name = "Test")).name)
        }

    @Test
    fun `GIVEN mutable fake with property WHEN reconfigured THEN property value changes`() {
        // GIVEN: A mutable repository with initial status
        val fake = fakeMutableRepository {
            status { "initializing" }
        }
        assertEquals("initializing", fake.status)

        // WHEN: Reconfiguring the property behavior
        fake.configure {
            status { "ready" }
        }

        // THEN: Property returns new value
        assertEquals("ready", fake.status)
    }

    @Test
    fun `GIVEN mutable fake WHEN configure called multiple times THEN last config wins`() {
        // GIVEN: A mutable repository
        val fake = fakeMutableRepository {
            findById { User(id = "v1", name = "Version 1") }
            status { "v1" }
        }

        // WHEN: Reconfiguring multiple times
        fake.configure { findById { User(id = "v2", name = "Version 2") } }
        fake.configure { findById { User(id = "v3", name = "Version 3") } }

        // THEN: Last configuration wins
        assertEquals("Version 3", fake.findById("any")?.name)
        // Status was never reconfigured, so it stays as initial
        assertEquals("v1", fake.status)
    }

    @Test
    fun `GIVEN mutable fake WHEN configure with selective update THEN only updated behaviors change`() {
        // GIVEN: A mutable repository with all behaviors configured
        val fake = fakeMutableRepository {
            findById { id -> User(id = id, name = "Found") }
            delete { true }
            status { "active" }
        }

        // WHEN: Only reconfiguring findById
        fake.configure {
            findById { null }
        }

        // THEN: Only findById changed, others remain
        assertNull(fake.findById("any"))
        assertTrue(fake.delete("any")) // unchanged
        assertEquals("active", fake.status) // unchanged
    }

    // ==========================================
    // MutabilityMode.IMMUTABLE Tests
    // ==========================================

    @Test
    fun `GIVEN immutable fake WHEN used THEN works normally`() {
        // GIVEN: An immutable service
        val fake = fakeImmutableService {
            process { value -> "processed: $value" }
        }

        // WHEN: Using the fake
        val result = fake.process("test")

        // THEN: It works correctly
        assertEquals("processed: test", result)
        // Note: fake.configure {} would not compile as it's not generated
    }

    // ==========================================
    // MutabilityMode.DEFAULT Tests (follows plugin default = IMMUTABLE)
    // ==========================================

    @Test
    fun `GIVEN default mutability WHEN used THEN follows plugin default`() {
        // GIVEN: A service with default mutability (plugin default = IMMUTABLE)
        val fake = fakeDefaultMutabilityService {
            execute { true }
        }

        // WHEN: Using the fake
        val result = fake.execute("test")

        // THEN: It works correctly (immutable, no configure method)
        assertTrue(result)
        // Note: fake.configure {} would not compile (default is immutable)
    }

    // ==========================================
    // Mutable + Call History Combined Tests
    // ==========================================

    @Test
    fun `GIVEN mutable fake with call history WHEN reconfigured THEN call history persists`() {
        // GIVEN: A mutable tracked repository
        val fake = fakeMutableTrackedRepository {
            find { listOf(User(id = "1", name = "Test")) }
            count { 1 }
        }

        // Make some calls
        fake.find("query1")
        fake.find("query2")
        assertEquals(2, fake.findCalls.value.size)

        // WHEN: Reconfiguring the behavior
        fake.configure {
            find { emptyList() }
        }

        // THEN: New behavior applies
        assertEquals(emptyList(), fake.find("query3"))
        // Call history includes calls from before AND after reconfiguration
        assertEquals(3, fake.findCalls.value.size)
        // count behavior was not reconfigured, still works
        assertEquals(1, fake.count())
    }

    @Test
    fun `GIVEN mutable tracked fake WHEN reconfigured THEN call count continues accumulating`() {
        // GIVEN: A mutable tracked repository
        val fake = fakeMutableTrackedRepository {
            count { 10 }
        }

        // Initial calls
        fake.count()
        fake.count()
        assertEquals(2, fake.countCalls.value.size)

        // WHEN: Reconfiguring
        fake.configure {
            count { 20 }
        }

        // More calls
        fake.count()
        assertEquals(20, fake.count())

        // THEN: Call count accumulated across reconfiguration
        assertEquals(4, fake.countCalls.value.size)
    }
}
