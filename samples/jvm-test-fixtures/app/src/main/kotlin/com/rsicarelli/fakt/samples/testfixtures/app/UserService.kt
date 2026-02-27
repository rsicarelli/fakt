// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.testfixtures.app

import com.rsicarelli.fakt.samples.testfixtures.core.CacheManager
import com.rsicarelli.fakt.samples.testfixtures.core.UserRepository
import com.rsicarelli.fakt.samples.testfixtures.core.models.User

/**
 * Service that depends on core interfaces.
 *
 * Tests for this class use fakes from core's testFixtures,
 * demonstrating cross-module fake sharing via Gradle test fixtures.
 */
class UserService(
    private val userRepository: UserRepository,
    private val cacheManager: CacheManager,
) {
    fun getUser(id: String): User? {
        val cached = cacheManager.get("user:$id")
        if (cached != null) return null // Simplified: cache hit means skip

        val user = userRepository.findById(id)
        if (user != null) {
            cacheManager.put("user:$id", user.name)
        }
        return user
    }

    fun getAllUsers(): List<User> = userRepository.findAll()

    fun createUser(name: String, email: String): User {
        val user = User(id = "", name = name, email = email)
        val saved = userRepository.save(user)
        cacheManager.put("user:${saved.id}", saved.name)
        return saved
    }

    fun deleteUser(id: String): Boolean {
        val deleted = userRepository.delete(id)
        if (deleted) {
            cacheManager.invalidate("user:$id")
        }
        return deleted
    }
}
