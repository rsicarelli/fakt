// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive test for CompanyService fake generation.
 *
 * Validates:
 * - Custom annotation support (@CompanyTestDouble instead of @Fake)
 * - Property configuration (companyName)
 * - Methods with various return types (Int, Boolean, Double)
 * - Method parameters (String)
 */
class CompanyServiceTest {
    @Test
    fun `GIVEN CompanyService WHEN accessing companyName THEN should return configured value`() {
        // Given
        val fake =
            fakeCompanyService {
                companyName { "TechCorp Inc." }
            }

        // When
        val name = fake.companyName

        // Then
        assertEquals("TechCorp Inc.", name)
    }

    @Test
    fun `GIVEN CompanyService WHEN getting employee count THEN should return configured number`() {
        // Given
        val fake =
            fakeCompanyService {
                getEmployeeCount { 150 }
            }

        // When
        val count = fake.getEmployeeCount()

        // Then
        assertEquals(150, count)
    }

    @Test
    fun `GIVEN CompanyService WHEN adding employee THEN should return boolean result`() {
        // Given
        var employeeAdded = false
        val fake =
            fakeCompanyService {
                addEmployee { name ->
                    if (name.isNotBlank()) {
                        employeeAdded = true
                        true
                    } else {
                        false
                    }
                }
            }

        // When
        val resultValid = fake.addEmployee("John Doe")
        val resultInvalid = fake.addEmployee("")

        // Then
        assertTrue(resultValid)
        assertFalse(resultInvalid)
        assertTrue(employeeAdded)
    }

    @Test
    fun `GIVEN CompanyService with custom annotation WHEN configuring multiple methods THEN should work correctly`() {
        // Given
        val fake =
            fakeCompanyService {
                companyName { "Global Solutions Ltd." }
                getEmployeeCount { 500 }
                getCompanySize { 3 } // 3 offices
                getAverageAge { 32.5 }
            }

        // When
        val name = fake.companyName
        val employeeCount = fake.getEmployeeCount()
        val companySize = fake.getCompanySize()
        val averageAge = fake.getAverageAge()

        // Then
        assertEquals("Global Solutions Ltd.", name)
        assertEquals(500, employeeCount)
        assertEquals(3, companySize)
        assertEquals(32.5, averageAge)
    }

    @Test
    fun `GIVEN CompanyService WHEN using defaults THEN should have sensible default values`() {
        // Given
        val fake = fakeCompanyService()

        // When
        val name = fake.companyName
        val employeeCount = fake.getEmployeeCount()
        val addResult = fake.addEmployee("Test Employee")
        val companySize = fake.getCompanySize()
        val averageAge = fake.getAverageAge()

        // Then
        assertEquals("", name) // Default: empty string
        assertEquals(0, employeeCount) // Default: 0
        assertFalse(addResult) // Default: false
        assertEquals(0, companySize) // Default: 0
        assertEquals(0.0, averageAge) // Default: 0.0
    }
}
