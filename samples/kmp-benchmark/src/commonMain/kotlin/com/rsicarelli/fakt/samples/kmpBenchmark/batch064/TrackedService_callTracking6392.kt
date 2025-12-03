// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch064

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.kmpBenchmark.models.User

@Fake
interface TrackedService_callTracking6392 {
    
    fun simpleMethod(): String

    
    suspend fun asyncMethod(): String

    
    fun <T> genericMethod(value: T): T

    
    fun methodWithParams(id: String, count: Int): Boolean

    
    val readOnlyProperty: String

    
    var mutableProperty: Int

    
    fun nullableMethod(): String?

    
    fun customTypeMethod(user: User): User

    
    suspend fun batchProcess(items: List<String>): List<String>

    
    fun varargsMethod(vararg values: String): Int

    
    fun nullableParamMethod(value: String?): Boolean

    
    suspend fun <T> asyncGenericMethod(value: T): T
}
