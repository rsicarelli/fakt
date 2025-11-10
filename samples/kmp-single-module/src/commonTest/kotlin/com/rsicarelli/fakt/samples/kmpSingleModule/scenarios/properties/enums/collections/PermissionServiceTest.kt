// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.properties.enums.collections

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PermissionServiceTest {
    @Test
    fun `GIVEN PermissionService fake WHEN configuring accessLevels THEN should return enum list`() {
        // Given
        val permissionService = fakePermissionService {
            accessLevels { listOf(AccessLevel.READ, AccessLevel.WRITE, AccessLevel.READ) }
        }

        // When
        val levels = permissionService.accessLevels

        // Then
        assertEquals(3, levels.size)
        assertEquals(listOf(AccessLevel.READ, AccessLevel.WRITE, AccessLevel.READ), levels)
    }

    @Test
    fun `GIVEN PermissionService fake WHEN configuring requiredPermissions THEN should return enum set`() {
        // Given
        val permissionService = fakePermissionService {
            requiredPermissions { setOf(AccessLevel.READ, AccessLevel.EXECUTE) }
        }

        // When
        val permissions = permissionService.requiredPermissions

        // Then
        assertEquals(2, permissions.size)
        assertTrue(permissions.contains(AccessLevel.READ))
        assertTrue(permissions.contains(AccessLevel.EXECUTE))
        assertFalse(permissions.contains(AccessLevel.ADMIN))
    }

    @Test
    fun `GIVEN PermissionService fake WHEN configuring permissionDescriptions THEN should return map with enum keys`() {
        // Given
        val descriptions = mapOf(
            AccessLevel.READ to "View content",
            AccessLevel.WRITE to "Modify content",
            AccessLevel.ADMIN to "Full control",
        )
        val permissionService = fakePermissionService {
            permissionDescriptions { descriptions }
        }

        // When
        val result = permissionService.permissionDescriptions

        // Then
        assertEquals(3, result.size)
        assertEquals("View content", result[AccessLevel.READ])
        assertEquals("Modify content", result[AccessLevel.WRITE])
        assertEquals("Full control", result[AccessLevel.ADMIN])
        assertNull(result[AccessLevel.EXECUTE])
    }

    @Test
    fun `GIVEN PermissionService fake WHEN configuring userPermissions THEN should return map with enum values`() {
        // Given
        val userPerms = mapOf(
            "user1" to AccessLevel.READ,
            "user2" to AccessLevel.ADMIN,
            "user3" to AccessLevel.WRITE,
        )
        val permissionService = fakePermissionService {
            userPermissions { userPerms }
        }

        // When
        val result = permissionService.userPermissions

        // Then
        assertEquals(3, result.size)
        assertEquals(AccessLevel.READ, result["user1"])
        assertEquals(AccessLevel.ADMIN, result["user2"])
        assertEquals(AccessLevel.WRITE, result["user3"])
        assertNull(result["user4"])
    }

    @Test
    fun `GIVEN PermissionService fake WHEN configuring optionalPermissions as null THEN should return null`() {
        // Given
        val permissionService = fakePermissionService {
            optionalPermissions { null }
        }

        // When
        val permissions = permissionService.optionalPermissions

        // Then
        assertNull(permissions)
    }

    @Test
    fun `GIVEN PermissionService fake WHEN configuring optionalPermissions THEN should return enum list`() {
        // Given
        val permissionService = fakePermissionService {
            optionalPermissions { listOf(AccessLevel.EXECUTE) }
        }

        // When
        val permissions = permissionService.optionalPermissions

        // Then
        assertEquals(1, permissions?.size)
        assertEquals(AccessLevel.EXECUTE, permissions?.first())
    }

    @Test
    fun `GIVEN PermissionService fake WHEN configuring getUserPermissions THEN should return user's permissions`() {
        // Given
        val permissionService = fakePermissionService {
            getUserPermissions { userId ->
                when (userId) {
                    "admin" -> listOf(
                        AccessLevel.READ,
                        AccessLevel.WRITE,
                        AccessLevel.EXECUTE,
                        AccessLevel.ADMIN
                    )

                    "editor" -> listOf(AccessLevel.READ, AccessLevel.WRITE)
                    "viewer" -> listOf(AccessLevel.READ)
                    else -> emptyList()
                }
            }
        }

        // When & Then
        assertEquals(4, permissionService.getUserPermissions("admin").size)
        assertEquals(2, permissionService.getUserPermissions("editor").size)
        assertEquals(1, permissionService.getUserPermissions("viewer").size)
        assertTrue(permissionService.getUserPermissions("unknown").isEmpty())
    }

    @Test
    fun `GIVEN PermissionService fake WHEN configuring hasAllPermissions THEN should check if user has required permissions`() {
        // Given
        val userPermissions = mapOf(
            "admin" to listOf(AccessLevel.READ, AccessLevel.WRITE, AccessLevel.ADMIN),
            "editor" to listOf(AccessLevel.READ, AccessLevel.WRITE),
        )

        val permissionService = fakePermissionService {
            hasAllPermissions { userId, required ->
                val userPerms = userPermissions[userId] ?: emptyList()
                required.all { it in userPerms }
            }
        }

        // When & Then
        assertTrue(
            permissionService.hasAllPermissions(
                "admin",
                setOf(AccessLevel.READ, AccessLevel.WRITE)
            )
        )
        assertTrue(permissionService.hasAllPermissions("editor", setOf(AccessLevel.READ)))
        assertFalse(permissionService.hasAllPermissions("editor", setOf(AccessLevel.ADMIN)))
    }

    @Test
    fun `GIVEN PermissionService fake WHEN configuring grantPermissions THEN should grant multiple permissions`() {
        // Given
        val grantedPermissions = mutableMapOf<String, MutableList<AccessLevel>>()
        val permissionService = fakePermissionService {
            grantPermissions { userId, permissions ->
                grantedPermissions.getOrPut(userId) { mutableListOf() }.addAll(permissions)
                true
            }
        }

        // When
        val result =
            permissionService.grantPermissions("user1", listOf(AccessLevel.READ, AccessLevel.WRITE))

        // Then
        assertTrue(result)
        assertEquals(2, grantedPermissions["user1"]?.size)
    }

    @Test
    fun `GIVEN PermissionService fake WHEN configuring getPermissionGroups THEN should return nested enum lists`() {
        // Given
        val permissionService = fakePermissionService {
            getPermissionGroups {
                listOf(
                    listOf(AccessLevel.READ, AccessLevel.WRITE), // Basic permissions
                    listOf(AccessLevel.EXECUTE), // Advanced permissions
                    listOf(AccessLevel.ADMIN), // Admin permissions
                )
            }
        }

        // When
        val groups = permissionService.getPermissionGroups()

        // Then
        assertEquals(3, groups.size)
        assertEquals(2, groups[0].size)
        assertEquals(1, groups[1].size)
        assertEquals(AccessLevel.READ, groups[0][0])
        assertEquals(AccessLevel.EXECUTE, groups[1][0])
    }

    @Test
    fun `GIVEN PermissionService fake WHEN configuring getPermissionPriorities THEN should return enum to int map`() {
        // Given
        val priorities = mapOf(
            AccessLevel.ADMIN to 1,
            AccessLevel.WRITE to 2,
            AccessLevel.EXECUTE to 3,
            AccessLevel.READ to 4,
        )
        val permissionService = fakePermissionService {
            getPermissionPriorities { priorities }
        }

        // When
        val result = permissionService.getPermissionPriorities()

        // Then
        assertEquals(4, result.size)
        assertEquals(1, result[AccessLevel.ADMIN])
        assertEquals(2, result[AccessLevel.WRITE])
        assertEquals(3, result[AccessLevel.EXECUTE])
        assertEquals(4, result[AccessLevel.READ])
    }
}
