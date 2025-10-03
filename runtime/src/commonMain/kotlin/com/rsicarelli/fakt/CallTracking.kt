// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt

/**
 * Annotation for fine-grained call tracking configuration.
 *
 * This annotation provides detailed control over call tracking behavior when used
 * alongside @Fake(trackCalls = true). It can be applied to specific methods or
 * the entire class to customize tracking behavior.
 *
 * ## Usage Examples
 * ```kotlin
 * @Fake(trackCalls = true)
 * interface UserService {
 *     @CallTracking(captureArguments = false)
 *     fun getUser(id: String): User  // Only tracks calls, not arguments
 *
 *     @CallTracking(maxCalls = 100)
 *     fun updateUser(user: User)     // Limits stored calls to 100
 * }
 * ```
 *
 * @param captureArguments Whether to capture method arguments in call records.
 * When false, only tracks that the method was called without storing parameters.
 * Reduces memory usage for methods with large parameters.
 *
 * @param captureReturnValues Whether to capture method return values in call records.
 * When false, only tracks calls without storing return values.
 * Useful for methods with large return objects.
 *
 * @param maxCalls Maximum number of calls to store per method.
 * When limit is reached, oldest calls are removed (FIFO behavior).
 * Set to -1 for unlimited storage.
 *
 * @param enableTimestamps Whether to record timestamps for method calls.
 * When true, adds call timing information for performance testing.
 *
 * @param autoVerify Automatically verify calls during fake lifecycle.
 * When true, throws exceptions for unexpected call patterns.
 * Useful for strict testing scenarios.
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
public annotation class CallTracking(
    val captureArguments: Boolean = true,
    val captureReturnValues: Boolean = false,
    val maxCalls: Int = -1,
    val enableTimestamps: Boolean = false,
    val autoVerify: Boolean = false,
)
