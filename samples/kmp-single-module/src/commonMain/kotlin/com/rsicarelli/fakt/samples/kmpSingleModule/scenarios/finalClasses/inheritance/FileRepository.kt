// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.samples.kmpSingleModule.scenarios.finalClasses.inheritance

import com.rsicarelli.fakt.Fake

/**
 * P1 Scenario: ClassExtendingAbstractClass
 *
 * **Pattern**: Open class extending abstract class with inheritance hierarchy
 * **Priority**: P1 (High - Common Inheritance Pattern)
 *
 * **What it tests**:
 * - Open class can extend abstract class and be faked
 * - Inherited abstract methods get error defaults (must configure)
 * - Own open methods get super call defaults (optional override)
 * - Multi-source member resolution (inherited + own)
 *
 * **Expected behavior**:
 * ```kotlin
 * // Inherited abstract methods from AbstractRepository
 * private var findByIdBehavior: (String) -> String? = { _ -> error("Configure findById behavior") }
 * private var saveBehavior: (String) -> Unit = { _ -> error("Configure save behavior") }
 *
 * // Own open methods
 * private var findAllBehavior: () -> List<String> = { super.findAll() }
 * private var clearCacheBehavior: () -> Unit = { super.clearCache() }
 * ```
 *
 * **Real-world equivalent**:
 * ```kotlin
 * class FileRepository : AbstractRepository() {
 *     override fun findById(id: String): String? = TODO()
 *     override fun save(entity: String) = TODO()
 *     open fun findAll(): List<String> = emptyList()
 *     open fun clearCache() = Unit
 * }
 * ```
 */

// Base abstract class with abstract methods
abstract class AbstractRepository {
    abstract fun findById(id: String): String?

    abstract fun save(entity: String)
}

// Derived open class with inherited abstract + own open methods
@Fake
open class FileRepository : AbstractRepository() {
    // Must implement abstract methods from parent
    override fun findById(id: String): String? {
        // Simulate database lookup
        return null
    }

    override fun save(entity: String) {
        // Simulate saving to file
    }

    // Own open methods
    open fun findAll(): List<String> {
        // Simulate listing all files
        return emptyList()
    }

    open fun clearCache() {
        // Simulate cache clearing
    }
}
