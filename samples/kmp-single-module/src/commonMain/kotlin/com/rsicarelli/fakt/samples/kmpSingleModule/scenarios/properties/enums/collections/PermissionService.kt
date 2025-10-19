// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.properties.enums.collections

import com.rsicarelli.fakt.Fake

/**
 * Permission service using enum collections.
 *
 * Tests:
 * - List<Enum> properties and return types
 * - Set<Enum> for unique permissions
 * - Map<Enum, T> with enum keys
 * - Map<T, Enum> with enum values
 * - Nested collections: List<List<Enum>>
 * - Nullable enum collections: List<Enum?>
 */
@Fake
interface PermissionService {
    /**
     * List of access levels (allows duplicates).
     */
    val accessLevels: List<AccessLevel>

    /**
     * Set of unique required permissions.
     */
    val requiredPermissions: Set<AccessLevel>

    /**
     * Map from access level to description.
     */
    val permissionDescriptions: Map<AccessLevel, String>

    /**
     * Map from user ID to their access level.
     */
    val userPermissions: Map<String, AccessLevel>

    /**
     * Nullable list of optional permissions.
     */
    val optionalPermissions: List<AccessLevel>?

    /**
     * Get all permissions for a user.
     */
    fun getUserPermissions(userId: String): List<AccessLevel>

    /**
     * Check if user has all required permissions.
     */
    fun hasAllPermissions(
        userId: String,
        required: Set<AccessLevel>,
    ): Boolean

    /**
     * Grant permissions to a user.
     */
    fun grantPermissions(
        userId: String,
        permissions: List<AccessLevel>,
    ): Boolean

    /**
     * Get permission levels grouped by category.
     * Tests nested collections: List<List<AccessLevel>>
     */
    fun getPermissionGroups(): List<List<AccessLevel>>

    /**
     * Get permissions with their priorities.
     * Tests Map<AccessLevel, Int>
     */
    fun getPermissionPriorities(): Map<AccessLevel, Int>
}

/**
 * Access levels for permission management.
 * Used to test enum collections (List, Set, Map).
 */
enum class AccessLevel {
    READ,
    WRITE,
    EXECUTE,
    ADMIN,
}

