// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch027

import com.rsicarelli.fakt.samples.kmpBenchmark.models.HttpStatus
import com.rsicarelli.fakt.Fake

@Fake
interface HttpService_properties_enums_richEnums2614 {
    
    val defaultSuccessStatus: HttpStatus

    
    val customErrorStatus: HttpStatus?

    
    fun sendRequest(url: String): HttpStatus

    
    fun isRetriable(status: HttpStatus): Boolean

    
    fun getStatusByCode(code: Int): HttpStatus?

    
    fun formatResponse(
        status: HttpStatus,
        body: String,
    ): String
}
