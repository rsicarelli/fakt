// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package test.sample

import com.rsicarelli.fakt.Fake

/**
 * Method-only interface with lambda parameters and suspend functions.
 *
 * Tests event processing pattern with:
 * - Lambda/function type parameters ((String) -> String, (String) -> Boolean)
 * - Callback patterns (onComplete, onError)
 * - Suspend function with lambda parameter
 * Validates function types without generic type parameters.
 */
@Fake
interface EventProcessor {
    fun processString(
        item: String,
        processor: (String) -> String,
    ): String

    fun processInt(
        item: Int,
        processor: (Int) -> String,
    ): String

    fun filter(
        items: List<String>,
        predicate: (String) -> Boolean,
    ): List<String>

    fun onComplete(callback: () -> Unit)

    fun onError(errorHandler: (Exception) -> Unit)

    suspend fun processAsync(
        item: String,
        processor: suspend (String) -> String,
    ): String
}
