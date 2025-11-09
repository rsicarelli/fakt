// Copyright (C) 2025 Rodrigo Sicarelli.
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.codegen.builder

import com.rsicarelli.fakt.codegen.model.CodeType

/**
 * Parses a type string into a [CodeType] structure.
 *
 * Supports:
 * - Simple types: "String", "Int", "User"
 * - Generic types: "List<String>", "Map<String, Int>"
 * - Nullable types: "String?", "User?"
 * - Nested generics: "List<Map<String, Int>>"
 *
 * Note: This is a simple parser for common cases.
 * Complex types may need explicit CodeType construction.
 *
 * @param typeString The type as a string
 * @return Parsed [CodeType]
 */
internal fun parseType(typeString: String): CodeType {
    return when {
        typeString.endsWith("?") ->
            CodeType.Nullable(parseType(typeString.dropLast(1)))

        typeString.contains("<") -> {
            val name = typeString.substringBefore("<")
            val argsString = typeString.substringAfter("<")
                .substringBeforeLast(">")
            val args = splitTypeArguments(argsString)
                .map { parseType(it.trim()) }
            CodeType.Generic(name, args)
        }

        else -> CodeType.Simple(typeString)
    }
}

/**
 * Splits generic type arguments by comma, respecting nested generics.
 *
 * Example:
 * - "String, Int" → ["String", "Int"]
 * - "Map<String, Int>, List<User>" → ["Map<String, Int>", "List<User>"]
 *
 * @param argsString Comma-separated type arguments
 * @return List of type argument strings
 */
private fun splitTypeArguments(argsString: String): List<String> {
    if (argsString.isBlank()) return emptyList()

    val result = mutableListOf<String>()
    var current = StringBuilder()
    var depth = 0

    for (char in argsString) {
        when (char) {
            '<' -> {
                depth++
                current.append(char)
            }

            '>' -> {
                depth--
                current.append(char)
            }

            ',' -> {
                if (depth == 0) {
                    result.add(current.toString())
                    current = StringBuilder()
                } else {
                    current.append(char)
                }
            }

            else -> current.append(char)
        }
    }

    if (current.isNotEmpty()) {
        result.add(current.toString())
    }

    return result
}
