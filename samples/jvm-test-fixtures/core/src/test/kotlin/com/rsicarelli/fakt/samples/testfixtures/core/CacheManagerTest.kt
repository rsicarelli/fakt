// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheManagerTest {
    @Test
    fun `GIVEN CacheManager fake WHEN getting cached value THEN returns configured value`() {
        val cache = mutableMapOf<String, String>()

        val fake = fakeCacheManager {
            get { key -> cache[key] }
            put { key, value -> cache[key] = value }
        }

        fake.put("key1", "value1")
        val result = fake.get("key1")

        assertEquals("value1", result)
    }

    @Test
    fun `GIVEN CacheManager fake with defaults WHEN getting value THEN returns null`() {
        val fake = fakeCacheManager()

        assertNull(fake.get("any-key"))
    }

    @Test
    fun `GIVEN CacheManager fake WHEN invalidating THEN calls configured behavior`() {
        val invalidated = mutableListOf<String>()

        val fake = fakeCacheManager {
            invalidate { key -> invalidated.add(key) }
        }

        fake.invalidate("key1")
        fake.invalidate("key2")

        assertEquals(listOf("key1", "key2"), invalidated)
    }
}
