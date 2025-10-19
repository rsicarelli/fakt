// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.core.storage

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Tests for KeyValueStorage fake generation and configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KeyValueStorageTest {

    @Test
    fun `GIVEN KeyValueStorage fake WHEN storing and retrieving string THEN should return stored value`() =
        runTest {
            // Given
            val storedData = mutableMapOf<String, String>()
            val storage =
                fakeKeyValueStorage {
                    putString { key, value ->
                        storedData[key] = value
                    }
                    getString { key ->
                        storedData[key]
                    }
                }

            // When
            storage.putString("token", "abc123")
            val retrieved = storage.getString("token")

            // Then
            assertEquals("abc123", retrieved)
        }

    @Test
    fun `GIVEN KeyValueStorage fake WHEN storing and retrieving int THEN should return stored value`() =
        runTest {
            // Given
            val storedData = mutableMapOf<String, Int>()
            val storage =
                fakeKeyValueStorage {
                    putInt { key, value ->
                        storedData[key] = value
                    }
                    getInt { key ->
                        storedData[key]
                    }
                }

            // When
            storage.putInt("user_id", 42)
            val retrieved = storage.getInt("user_id")

            // Then
            assertEquals(42, retrieved)
        }

    @Test
    fun `GIVEN KeyValueStorage fake WHEN storing and retrieving boolean THEN should return stored value`() =
        runTest {
            // Given
            val storedData = mutableMapOf<String, Boolean>()
            val storage =
                fakeKeyValueStorage {
                    putBoolean { key, value ->
                        storedData[key] = value
                    }
                    getBoolean { key ->
                        storedData[key]
                    }
                }

            // When
            storage.putBoolean("is_logged_in", true)
            val retrieved = storage.getBoolean("is_logged_in")

            // Then
            assertTrue(retrieved ?: false)
        }

    @Test
    fun `GIVEN KeyValueStorage fake WHEN removing key THEN should not be retrievable`() =
        runTest {
            // Given
            val storedData = mutableMapOf<String, String>()
            val storage =
                fakeKeyValueStorage {
                    putString { key, value ->
                        storedData[key] = value
                    }
                    getString { key ->
                        storedData[key]
                    }
                    remove { key ->
                        storedData.remove(key)
                    }
                }

            storage.putString("token", "abc123")

            // When
            storage.remove("token")
            val retrieved = storage.getString("token")

            // Then
            assertNull(retrieved)
        }

    @Test
    fun `GIVEN KeyValueStorage fake WHEN checking contains THEN should return correct value`() =
        runTest {
            // Given
            val storedData = mutableMapOf<String, String>()
            val storage =
                fakeKeyValueStorage {
                    putString { key, value ->
                        storedData[key] = value
                    }
                    contains { key ->
                        storedData.containsKey(key)
                    }
                }

            storage.putString("token", "abc123")

            // When
            val hasToken = storage.contains("token")
            val hasOther = storage.contains("other")

            // Then
            assertTrue(hasToken)
            assertFalse(hasOther)
        }

    @Test
    fun `GIVEN KeyValueStorage fake WHEN clearing THEN should remove all data`() =
        runTest {
            // Given
            val storedData = mutableMapOf<String, String>()
            val storage =
                fakeKeyValueStorage {
                    putString { key, value ->
                        storedData[key] = value
                    }
                    getString { key ->
                        storedData[key]
                    }
                    clear {
                        storedData.clear()
                    }
                }

            storage.putString("key1", "value1")
            storage.putString("key2", "value2")

            // When
            storage.clear()
            val retrieved1 = storage.getString("key1")
            val retrieved2 = storage.getString("key2")

            // Then
            assertNull(retrieved1)
            assertNull(retrieved2)
        }
}

/**
 * Tests for Cache fake generation and configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheTest {

    @Test
    fun `GIVEN Cache fake WHEN putting and getting value THEN should return cached value`() {
        // Given
        val cacheData = mutableMapOf<String, Any>()
        val cache =
            fakeCache {
                put { key, value, _ ->
                    cacheData[key] = value
                }
                get { key ->
                    cacheData[key]
                }
            }

        // When
        cache.put("user_123", "John Doe")
        val retrieved = cache.get("user_123")

        // Then
        assertEquals("John Doe", retrieved)
    }

    @Test
    fun `GIVEN Cache fake WHEN removing value THEN should not be retrievable`() {
        // Given
        val cacheData = mutableMapOf<String, Any>()
        val cache =
            fakeCache {
                put { key, value, _ ->
                    cacheData[key] = value
                }
                get { key ->
                    cacheData[key]
                }
                remove { key ->
                    cacheData.remove(key)
                }
            }

        cache.put("user_123", "John Doe")

        // When
        cache.remove("user_123")
        val retrieved = cache.get("user_123")

        // Then
        assertNull(retrieved)
    }

    @Test
    fun `GIVEN Cache fake WHEN checking size THEN should return correct count`() {
        // Given
        val cacheData = mutableMapOf<String, Any>()
        val cache =
            fakeCache {
                put { key, value, _ ->
                    cacheData[key] = value
                }
                size { cacheData.size }
            }

        // When
        cache.put("key1", "value1")
        cache.put("key2", "value2")
        val size = cache.size

        // Then
        assertEquals(2, size)
    }

    @Test
    fun `GIVEN Cache fake WHEN checking contains THEN should return correct value`() {
        // Given
        val cacheData = mutableMapOf<String, Any>()
        val cache =
            fakeCache {
                put { key, value, _ ->
                    cacheData[key] = value
                }
                contains { key ->
                    cacheData.containsKey(key)
                }
            }

        cache.put("key1", "value1")

        // When
        val hasKey1 = cache.contains("key1")
        val hasKey2 = cache.contains("key2")

        // Then
        assertTrue(hasKey1)
        assertFalse(hasKey2)
    }
}

/**
 * Tests for SecureStorage fake generation and configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SecureStorageTest {

    @Test
    fun `GIVEN SecureStorage fake WHEN storing and retrieving secure value THEN should return decrypted value`() =
        runTest {
            // Given
            val secureData = mutableMapOf<String, String>()
            val storage =
                fakeSecureStorage {
                    putSecure { key, value ->
                        // Simulate encryption
                        secureData[key] = "encrypted_$value"
                    }
                    getSecure { key ->
                        // Simulate decryption
                        secureData[key]?.removePrefix("encrypted_")
                    }
                }

            // When
            storage.putSecure("api_key", "secret123")
            val retrieved = storage.getSecure("api_key")

            // Then
            assertEquals("secret123", retrieved)
        }

    @Test
    fun `GIVEN SecureStorage fake WHEN removing secure value THEN should not be retrievable`() =
        runTest {
            // Given
            val secureData = mutableMapOf<String, String>()
            val storage =
                fakeSecureStorage {
                    putSecure { key, value ->
                        secureData[key] = value
                    }
                    getSecure { key ->
                        secureData[key]
                    }
                    removeSecure { key ->
                        secureData.remove(key)
                    }
                }

            storage.putSecure("password", "secret")

            // When
            storage.removeSecure("password")
            val retrieved = storage.getSecure("password")

            // Then
            assertNull(retrieved)
        }

    @Test
    fun `GIVEN SecureStorage fake WHEN checking isAvailable THEN should return configured value`() {
        // Given
        val storage =
            fakeSecureStorage {
                isAvailable { true }
            }

        // When
        val available = storage.isAvailable

        // Then
        assertTrue(available)
    }
}
