// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch006

import com.rsicarelli.fakt.Fake

@Fake
interface PermissionService_properties_enums_collections522 {
    
    val accessLevels: List<PermissionService_properties_enums_collections522_1>

    
    val requiredPermissions: Set<PermissionService_properties_enums_collections522_1>

    
    val permissionDescriptions: Map<PermissionService_properties_enums_collections522_1, String>

    
    val userPermissions: Map<String, PermissionService_properties_enums_collections522_1>

    
    val optionalPermissions: List<PermissionService_properties_enums_collections522_1>?

    
    fun getUserPermissions(userId: String): List<PermissionService_properties_enums_collections522_1>

    
    fun hasAllPermissions(
        userId: String,
        required: Set<PermissionService_properties_enums_collections522_1>,
    ): Boolean

    
    fun grantPermissions(
        userId: String,
        permissions: List<PermissionService_properties_enums_collections522_1>,
    ): Boolean

    
    fun getPermissionGroups(): List<List<PermissionService_properties_enums_collections522_1>>

    
    fun getPermissionPriorities(): Map<PermissionService_properties_enums_collections522_1, Int>
}

enum class PermissionService_properties_enums_collections522_1 {
    READ,
    WRITE,
    EXECUTE,
    ADMIN,
}

