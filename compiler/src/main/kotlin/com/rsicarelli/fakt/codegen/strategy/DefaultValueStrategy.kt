// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.strategy

import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeType

/**
 * Strategy for generating default values for Kotlin types.
 *
 * Implementations provide intelligent defaults for different type categories:
 * - Primitives: String → "", Int → 0
 * - Collections: List → emptyList()
 * - Stdlib: StateFlow → MutableStateFlow(default)
 * - Nullable: T? → null
 *
 * Strategies can be composed to handle complex types.
 *
 * Example:
 * ```kotlin
 * val strategy = PrimitiveDefaultStrategy()
 * val type = CodeType.Simple("String")
 *
 * if (strategy.supports(type)) {
 *     val default = strategy.defaultValue(type)  // StringLiteral("")
 * }
 * ```
 *
 * @see PrimitiveDefaultStrategy
 * @see CollectionDefaultStrategy
 * @see StdlibDefaultStrategy
 * @see NullableDefaultStrategy
 * @see DefaultValueResolver
 */
public interface DefaultValueStrategy {
    /**
     * Checks if this strategy can provide a default for the given type.
     *
     * @param type The type to check
     * @return true if this strategy supports the type, false otherwise
     */
    public fun supports(type: CodeType): Boolean

    /**
     * Generates a default value expression for the given type.
     *
     * Must only be called if [supports] returns true for this type.
     * Throws [IllegalArgumentException] if type is not supported.
     *
     * @param type The type to generate a default for
     * @return CodeExpression representing the default value
     * @throws IllegalArgumentException if type is not supported by this strategy
     */
    public fun defaultValue(type: CodeType): CodeExpression
}
