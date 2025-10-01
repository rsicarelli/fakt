// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

// Test custom company annotation
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class CompanyTestDouble

@CompanyTestDouble
interface CompanyService {
    val companyName: String
    fun getEmployeeCount(): Int
    fun addEmployee(name: String): Boolean
    fun getCompanySize(): Int  // Added new method
    fun getAverageAge(): Double  // New method for incremental test
}

@CompanyTestDouble
interface EnterpriseRepository {
    suspend fun findData(query: String): List<String>
    fun saveData(data: String): Result<Unit>
    fun deleteData(id: String): Boolean  // New method to trigger incremental
}
