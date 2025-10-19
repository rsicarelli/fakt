package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.metaAnnotations

import com.rsicarelli.fakt.samples.kmpSingleModule.models.CustomAnnotation

/**
 * Custom annotation with suspend + Result types.
 *
 * Tests custom annotation (@CompanyTestDouble) combined with:
 * - Suspend functions
 * - Result type for error handling
 * - Boolean return types
 * Validates custom annotations work with complex feature combinations.
 */
@CustomAnnotation
interface EnterpriseRepository {
    suspend fun findData(query: String): List<String>

    fun saveData(data: String): Result<Unit>

    fun deleteData(id: String): Boolean // New method to trigger incremental
}