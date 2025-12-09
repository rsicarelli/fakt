// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.strategy

import com.rsicarelli.fakt.codegen.model.CodeExpression
import com.rsicarelli.fakt.codegen.model.CodeType

/**
 * Strategy for generating default values for nullable types.
 *
 * All nullable types default to `null`, regardless of the inner type.
 *
 * This strategy should have the highest priority in composition,
 * as nullable types always have a clear default value.
 *
 * Example:
 * ```kotlin
 * val strategy = NullableDefaultStrategy()
 *
 * val type1 = CodeType.Nullable(CodeType.Simple("String"))
 * strategy.defaultValue(type1)  // Raw("null")
 *
 * val type2 = CodeType.Nullable(CodeType.Generic("List", listOf(CodeType.Simple("Int"))))
 * strategy.defaultValue(type2)  // Raw("null")
 * ```
 */
public class NullableDefaultStrategy : DefaultValueStrategy {
    override fun supports(type: CodeType): Boolean = type is CodeType.Nullable

    override fun defaultValue(type: CodeType): CodeExpression {
        require(supports(type)) {
            "NullableDefaultStrategy does not support type: $type"
        }

        return CodeExpression.Raw("null")
    }
}
