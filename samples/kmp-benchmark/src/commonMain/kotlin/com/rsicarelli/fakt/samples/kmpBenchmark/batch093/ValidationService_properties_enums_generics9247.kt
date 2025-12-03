// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch093

import com.rsicarelli.fakt.samples.kmpBenchmark.models.ValidationStatus
import com.rsicarelli.fakt.Fake

@Fake
interface ValidationService_properties_enums_generics9247 {
    
    fun validate(input: String): Result<ValidationStatus>

    
    fun validateBatch(inputs: List<String>): Result<ValidationStatus?>

    
    fun hasAnyStatus(
        current: ValidationStatus,
        vararg allowed: ValidationStatus,
    ): Boolean

    
    fun getStatusWithTimestamp(input: String): Pair<ValidationStatus, Long>

    
    fun getValidationDetails(input: String): Triple<ValidationStatus, String, Int>

    
    fun combineStatuses(vararg statuses: ValidationStatus): Result<ValidationStatus>

    
    fun getOptionalStatus(input: String): ValidationStatus?
}
