// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.singlemodule.scenarios.finalClasses.generics

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: GenericOpenClass
 *
 * **Pattern**: Open class with class-level generic type parameter
 * **Priority**: P1 (High - Common Container/Repository Pattern)
 *
 * **What it tests**:
 * - Class-level generic type parameter T
 * - Generic type used in method signatures
 * - Generic type used in property signatures
 * - Type safety preservation in fakes
 *
 * **Expected behavior**:
 * ```kotlin
 * // NOTE: This may require compiler enhancement for class-level generics
 * // Currently, we expect Either:
 * // 1. Full generic support (ideal): Container<T>
 * // 2. Type erasure: Container<Any> with warnings
 * // 3. Skip generation with clear error message
 *
 * class FakeContainerImpl<T> : Container<T>() {
 *     private var getBehavior: () -> T? = { super.get() }
 *     private var setBehavior: (T) -> Unit = { value -> super.set(value) }
 *     private var itemsBehavior: () -> List<T> = { super.items }
 *
 *     override fun get(): T? = getBehavior()
 *     override fun set(value: T) = setBehavior(value)
 *     override val items: List<T> get() = itemsBehavior()
 * }
 * ```
 *
 * **Real-world equivalent**:
 * ```kotlin
 * open class Container<T> {
 *     open fun get(): T? = null
 *     open fun set(value: T) = Unit
 *     open val items: List<T> = emptyList()
 * }
 *
 * // Usage
 * val stringContainer: Container<String> = ...
 * val userContainer: Container<User> = ...
 * ```
 */
@Fake
open class Container<T> {
    // Generic type in return type
    open fun get(): T? = null

    // Generic type in parameter
    open fun set(value: T) {
        // Store value (simulated)
    }

    // Generic type in property
    open val items: List<T>
        get() = emptyList()

    // Multiple generics in signature
    open fun contains(value: T): Boolean = false
}
