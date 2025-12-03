// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpBenchmark.batch064

import com.rsicarelli.fakt.Fake

@Fake
interface EdgeCaseService_callTracking6353 {
    
    fun recursiveMethod(depth: Int): Int

    
    fun highFrequencyMethod(value: Int): Int

    
    suspend fun cancellableMethod(delayMs: Long): String

    
    fun methodWithDefaults(
        required: String,
        optional1: Int = 42,
        optional2: Boolean = true,
    ): String

    
    fun throwingMethod(shouldThrow: Boolean): String

    
    val complexProperty: String

    
    var rapidSetProperty: Int

    
    suspend fun concurrentMethod(value: Int): Int

    
    fun memoryIntensiveMethod(iterations: Int): List<String>
}
