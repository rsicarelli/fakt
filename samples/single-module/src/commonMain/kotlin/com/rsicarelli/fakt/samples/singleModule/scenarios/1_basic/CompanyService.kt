// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.basic

import com.rsicarelli.fakt.samples.singleModule.models.CompanyTestDouble

/**
 * Custom annotation test (@CompanyTestDouble instead of @Fake).
 *
 * Tests support for custom annotations as alternatives to @Fake.
 * Validates that the compiler plugin can detect and process custom annotations
 * for organizational-specific naming preferences or multi-annotation scenarios.
 */
@CompanyTestDouble
interface CompanyService {
    val companyName: String

    fun getEmployeeCount(): Int

    fun addEmployee(name: String): Boolean

    fun getCompanySize(): Int // Added new method

    fun getAverageAge(): Double // New method for incremental test
}
