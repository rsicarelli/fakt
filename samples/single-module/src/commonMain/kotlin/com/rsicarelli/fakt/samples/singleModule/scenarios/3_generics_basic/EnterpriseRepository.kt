// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.generics_basic

import com.rsicarelli.fakt.samples.singleModule.models.CompanyTestDouble

/**
 * Custom annotation with suspend + Result types.
 *
 * Tests custom annotation (@CompanyTestDouble) combined with:
 * - Suspend functions
 * - Result type for error handling
 * - Boolean return types
 * Validates custom annotations work with complex feature combinations.
 */
@CompanyTestDouble
interface EnterpriseRepository {
    suspend fun findData(query: String): List<String>

    fun saveData(data: String): Result<Unit>

    fun deleteData(id: String): Boolean // New method to trigger incremental
}
