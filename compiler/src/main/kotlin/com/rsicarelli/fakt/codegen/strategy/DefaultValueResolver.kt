// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.strategy

import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeType

/**
 * Main facade for resolving default values for any Kotlin type.
 *
 * Composes all available strategies with priority ordering:
 * 1. [NullableDefaultStrategy] - nullable types always return null
 * 2. [PrimitiveDefaultStrategy] - String, Int, Boolean, etc.
 * 3. [StdlibDefaultStrategy] - Unit, Flow, StateFlow, Result
 * 4. [CollectionDefaultStrategy] - List, Set, Map, Array
 * 5. Fallback - TODO("No default for {type}")
 *
 * The resolver tries strategies in order until one supports the type.
 * If no strategy supports the type, returns a TODO expression.
 *
 * Example:
 * ```kotlin
 * val resolver = DefaultValueResolver()
 *
 * // Nullable wins over everything
 * resolver.resolve(CodeType.Nullable(CodeType.Simple("String")))  // null
 *
 * // Primitives
 * resolver.resolve(CodeType.Simple("String"))  // ""
 * resolver.resolve(CodeType.Simple("Int"))     // 0
 *
 * // Collections
 * resolver.resolve(CodeType.Generic("List", listOf(...)))  // emptyList()
 *
 * // Stdlib
 * resolver.resolve(CodeType.Generic("StateFlow", listOf(...)))  // MutableStateFlow(...)
 *
 * // Unknown types
 * resolver.resolve(CodeType.Simple("CustomType"))  // TODO("No default for CustomType")
 * ```
 */
public class DefaultValueResolver(
    private val classLevelTypeParams: Set<String> = emptySet()
) {

    private val strategies = listOf(
        NullableDefaultStrategy(),
        PrimitiveDefaultStrategy(),
        StdlibDefaultStrategy(),
        CollectionDefaultStrategy(classLevelTypeParams)
    )

    /**
     * Resolves a default value expression for the given type.
     *
     * Tries all strategies in priority order. If no strategy supports
     * the type, returns a TODO expression as fallback.
     *
     * @param type The type to resolve a default for
     * @return CodeExpression representing the default value
     */
    public fun resolve(type: CodeType): CodeExpression {
        for (strategy in strategies) {
            if (strategy.supports(type)) {
                return strategy.defaultValue(type)
            }
        }

        // Fallback for unsupported types
        return CodeExpression.Raw("TODO(\"No default for $type\")")
    }
}
