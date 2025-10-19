// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.samples.kmpmultimodule.core.storage

import com.rsicarelli.fakt.Fake

/**
 * Key-value storage interface for persistent data.
 *
 * In a real app, this would be implemented using:
 * - SharedPreferences/DataStore (Android)
 * - UserDefaults (iOS)
 * - LocalStorage (JS)
 * - Preferences (JVM Desktop)
 *
 * Example usage:
 * ```kotlin
 * storage.putString("user_token", "abc123")
 * val token = storage.getString("user_token")
 * ```
 */
@Fake
interface KeyValueStorage {
    /**
     * Store a string value.
     */
    suspend fun putString(key: String, value: String)

    /**
     * Retrieve a string value, or null if not found.
     */
    suspend fun getString(key: String): String?

    /**
     * Store an integer value.
     */
    suspend fun putInt(key: String, value: Int)

    /**
     * Retrieve an integer value, or null if not found.
     */
    suspend fun getInt(key: String): Int?

    /**
     * Store a boolean value.
     */
    suspend fun putBoolean(key: String, value: Boolean)

    /**
     * Retrieve a boolean value, or null if not found.
     */
    suspend fun getBoolean(key: String): Boolean?

    /**
     * Remove a key-value pair.
     */
    suspend fun remove(key: String)

    /**
     * Clear all stored data.
     */
    suspend fun clear()

    /**
     * Check if a key exists.
     */
    suspend fun contains(key: String): Boolean
}

/**
 * In-memory cache interface with expiration support.
 *
 * Used for temporary data caching to improve performance.
 * In a real app, this would be implemented using LRU cache with TTL.
 */
@Fake
interface Cache {
    /**
     * Put a value in the cache with optional TTL in milliseconds.
     */
    fun put(key: String, value: Any, ttlMillis: Long = 0)

    /**
     * Get a value from the cache, or null if not found or expired.
     */
    fun get(key: String): Any?

    /**
     * Remove a value from the cache.
     */
    fun remove(key: String)

    /**
     * Clear all cached data.
     */
    fun clear()

    /**
     * Check if a key exists in the cache and is not expired.
     */
    fun contains(key: String): Boolean

    /**
     * Get the number of items in the cache.
     */
    val size: Int
}

/**
 * Secure storage interface for sensitive data like tokens and passwords.
 *
 * In a real app, this would be implemented using:
 * - Keychain (iOS)
 * - Keystore (Android)
 * - Encrypted SharedPreferences (Android)
 * - SecretStorage (Linux)
 */
@Fake
interface SecureStorage {
    /**
     * Store a sensitive string value with encryption.
     */
    suspend fun putSecure(key: String, value: String)

    /**
     * Retrieve a sensitive string value, decrypting it.
     */
    suspend fun getSecure(key: String): String?

    /**
     * Remove a sensitive value.
     */
    suspend fun removeSecure(key: String)

    /**
     * Check if secure storage is available on this platform.
     */
    val isAvailable: Boolean
}
