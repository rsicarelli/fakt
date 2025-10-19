// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.callTracking

import com.rsicarelli.fakt.Fake
import com.rsicarelli.fakt.samples.singleModule.models.User

/**
 * Interface for comprehensive call tracking testing.
 *
 * Tests call tracking with MutableStateFlow for:
 * - Simple methods
 * - Suspend functions
 * - Generic methods (method-level type parameters)
 * - Methods with parameters
 * - Nullable return types
 * - Custom domain types
 * - Properties (val and var)
 * - Varargs
 * - Collection transformations
 */
@Fake
interface TrackedService {
    /**
     * Simple method with no parameters.
     */
    fun simpleMethod(): String

    /**
     * Suspend function for async operations.
     */
    suspend fun asyncMethod(): String

    /**
     * Method with method-level generic type parameter.
     */
    fun <T> genericMethod(value: T): T

    /**
     * Method with multiple parameters of different types.
     */
    fun methodWithParams(id: String, count: Int): Boolean

    /**
     * Read-only property.
     */
    val readOnlyProperty: String

    /**
     * Mutable property (tracks getter and setter separately).
     */
    var mutableProperty: Int

    /**
     * Method returning nullable type.
     */
    fun nullableMethod(): String?

    /**
     * Method with custom domain type.
     */
    fun customTypeMethod(user: User): User

    /**
     * Suspend function with collection transformation.
     */
    suspend fun batchProcess(items: List<String>): List<String>

    /**
     * Method with varargs parameter.
     */
    fun varargsMethod(vararg values: String): Int

    /**
     * Method with nullable parameter.
     */
    fun nullableParamMethod(value: String?): Boolean

    /**
     * Suspend method with generic type.
     */
    suspend fun <T> asyncGenericMethod(value: T): T
}
