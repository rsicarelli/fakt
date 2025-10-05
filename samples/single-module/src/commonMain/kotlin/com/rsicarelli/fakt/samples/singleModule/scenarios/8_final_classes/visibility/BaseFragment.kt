// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singleModule.scenarios.finalClasses.visibility

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: ClassWithProtectedMembers
 *
 * **Pattern**: Open class with protected open methods and properties
 * **Priority**: P1 (High - Common Framework/Library Pattern)
 *
 * **What it tests**:
 * - Protected open methods can be overridden
 * - Protected members are accessible within inheritance hierarchy
 * - Fake can override protected members
 * - Public methods can call protected methods (delegation pattern)
 *
 * **Expected behavior**:
 * ```kotlin
 * // Public open method
 * private var onCreateBehavior: () -> Unit = { super.onCreate() }
 *
 * // Protected open method
 * private var onInitBehavior: () -> Unit = { super.onInit() }
 *
 * // Protected open property
 * private var isInitializedBehavior: () -> Boolean = { super.isInitialized }
 * ```
 *
 * **Real-world equivalent**:
 * ```kotlin
 * open class BaseFragment {
 *     open fun onCreate() {
 *         onInit() // Call protected lifecycle method
 *     }
 *
 *     protected open fun onInit() {
 *         // Subclass lifecycle hook
 *     }
 *
 *     protected open val isInitialized: Boolean = true
 * }
 * ```
 */
@Fake
open class BaseFragment {
    // Public open method
    open fun onCreate() {
        // Call protected lifecycle method
        onInit()
        println("onCreate called, initialized=$isInitialized")
    }

    // Protected open method - lifecycle hook for subclasses
    protected open fun onInit() {
        // Default initialization logic
    }

    // Protected open property
    protected open val isInitialized: Boolean
        get() = true

    // Public method using protected member
    open fun checkInitialized(): Boolean {
        return isInitialized
    }
}
