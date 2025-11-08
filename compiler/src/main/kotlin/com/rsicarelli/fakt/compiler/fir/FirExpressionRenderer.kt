// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.types.ConstantValueKind

/**
 * Renders FirExpression to Kotlin source code strings.
 *
 * **Phase 3C.4**: Default Parameter Support
 *
 * This is a pragmatic, simple renderer that handles common default value patterns:
 * - Literals: strings, numbers, booleans, null
 * - Simple collections (future)
 * - Simple constructors (future)
 *
 * For complex expressions (function calls, property access, etc.), returns null to indicate
 * rendering is not supported. In such cases, the caller can decide to:
 * - Use null as the default
 * - Report a warning
 * - Skip the default parameter
 *
 * ## Design Philosophy
 *
 * This renderer follows the "90% solution" approach:
 * - Most real-world default parameters are simple literals
 * - Complex defaults (function calls, computed values) are rare in interface methods
 * - If rendering fails, we gracefully degrade (use null or omit default)
 *
 * ## Future Enhancements
 *
 * If more complex expressions are needed:
 * - Array/List literals: `listOf(1, 2, 3)`, `emptyList()`
 * - Map literals: `mapOf("key" to "value")`, `emptyMap()`
 * - Simple constructors: `User("default")`
 * - Property references: `someObject.property` (requires symbol resolution)
 *
 * @return Rendered Kotlin code string, or null if expression is too complex to render
 */
internal fun renderDefaultValue(expression: FirExpression?): String? {
    if (expression == null) return null

    return when (expression) {
        // Literal constants (most common case - handles all primitives, strings, null)
        is FirLiteralExpression -> renderLiteral(expression)

        // Function calls - complex, not supported yet
        is FirFunctionCall -> null

        // Property access - complex, not supported yet
        is FirPropertyAccessExpression -> null

        // Lambda expressions - complex, not supported yet
        is FirAnonymousFunctionExpression -> null

        // Unknown or complex expression - return null
        else -> null
    }
}

/**
 * Render FirLiteralExpression to Kotlin source code.
 *
 * Handles:
 * - String literals with proper escaping
 * - Numeric literals (Int, Long, Float, Double)
 * - Boolean literals
 * - Char literals
 */
private fun renderLiteral(literal: FirLiteralExpression): String? {
    return when (literal.kind) {
        ConstantValueKind.Null -> "null"

        ConstantValueKind.Boolean -> literal.value.toString()

        ConstantValueKind.Char -> "'${literal.value}'"

        ConstantValueKind.Byte -> "${literal.value}"

        ConstantValueKind.UnsignedByte -> "${literal.value}u"

        ConstantValueKind.Short -> "${literal.value}"

        ConstantValueKind.UnsignedShort -> "${literal.value}u"

        ConstantValueKind.Int -> "${literal.value}"

        ConstantValueKind.UnsignedInt -> "${literal.value}u"

        ConstantValueKind.Long -> "${literal.value}L"

        ConstantValueKind.UnsignedLong -> "${literal.value}uL"

        ConstantValueKind.String -> {
            // Escape the string value properly
            val stringValue = literal.value as? String ?: return null
            "\"${escapeString(stringValue)}\""
        }

        ConstantValueKind.Float -> "${literal.value}f"

        ConstantValueKind.Double -> "${literal.value}"

        // Not supported yet
        ConstantValueKind.IntegerLiteral,
        ConstantValueKind.UnsignedIntegerLiteral,
        ConstantValueKind.Error,
        -> null
    }
}

/**
 * Escape special characters in string literals.
 *
 * Handles common escape sequences:
 * - Backslash: \ → \\
 * - Quote: " → \"
 * - Newline: \n
 * - Tab: \t
 * - Carriage return: \r
 */
private fun escapeString(value: String): String {
    return value
        .replace("\\", "\\\\") // Backslash must be first
        .replace("\"", "\\\"") // Quote
        .replace("\n", "\\n") // Newline
        .replace("\t", "\\t") // Tab
        .replace("\r", "\\r") // Carriage return
}
