package com.rsicarelli.fakt.samples.singlemodule.scenarios.metaAnnotations

import com.rsicarelli.fakt.samples.singlemodule.models.CustomAnnotation

/**
 * Custom annotation test (@CompanyTestDouble instead of @Fake).
 *
 * Tests support for custom annotations as alternatives to @Fake.
 * Validates that the compiler plugin can detect and process custom annotations
 * for organizational-specific naming preferences or multi-annotation scenarios.
 */
@CustomAnnotation
interface CompanyService {
    val companyName: String

    fun getEmployeeCount(): Int

    fun addEmployee(name: String): Boolean

    fun getCompanySize(): Int // Added new method

    fun getAverageAge(): Double // New method for incremental test
}