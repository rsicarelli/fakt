// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

/**
 * Parsed type parameter information for generating extension functions.
 *
 * @property declaration Type parameter declaration (e.g., "<T, K : Any> ")
 * @property whereClause Where clause for constraints (e.g., " where T : CharSequence")
 * @property usage Type parameter usage on receiver (e.g., "<T, K>")
 */
internal data class TypeParamsInfo(
    val declaration: String,
    val whereClause: String,
    val usage: String,
)

/**
 * Parses class-level type parameters into declaration, where clause, and usage components.
 *
 * Handles variance modifiers (out/in), simple constraints, and multiple constraints
 * that require where clauses.
 */
internal fun parseTypeParameters(classTypeParameters: List<String>): TypeParamsInfo {
    if (classTypeParameters.isEmpty()) {
        return TypeParamsInfo(declaration = "", whereClause = "", usage = "")
    }

    val typeParamParts =
        classTypeParameters.map { param ->
            val cleanParam = param.removePrefix("out ").removePrefix("in ").trim()
            val name = cleanParam.substringBefore(" :").trim()
            val constraintPart = if (" :" in cleanParam) cleanParam.substringAfter(" :").trim() else null
            name to constraintPart
        }

    val needsWhereClause =
        typeParamParts.any { (_, constraint) ->
            constraint != null && ("," in constraint || "Comparable" in constraint)
        }

    val (declaration, whereClause) =
        if (needsWhereClause) {
            val params = typeParamParts.map { it.first }
            val whereConstraints =
                typeParamParts
                    .filter { it.second != null }
                    .flatMap { (name, constraints) ->
                        splitConstraints(constraints!!).map { "$name : $it" }
                    }.joinToString(", ")
            val where = if (whereConstraints.isNotEmpty()) " where $whereConstraints" else ""
            "<${params.joinToString(", ")}> " to where
        } else {
            val params =
                typeParamParts.map { (name, constraint) ->
                    if (constraint != null) "$name : $constraint" else name
                }
            "<${params.joinToString(", ")}> " to ""
        }

    val usage = "<${typeParamParts.joinToString(", ") { it.first }}>"

    return TypeParamsInfo(declaration = declaration, whereClause = whereClause, usage = usage)
}

/**
 * Splits constraint string by commas, but handles nested generic types correctly.
 *
 * Examples:
 * - "CharSequence, Comparable<T>" -> ["CharSequence", "Comparable<T>"]
 * - "Map<K, V>" -> ["Map<K, V>"]  (doesn't split inside generics)
 */
internal fun splitConstraints(constraints: String): List<String> {
    val result = mutableListOf<String>()
    var depth = 0
    var current = StringBuilder()

    for (char in constraints) {
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
                    result.add(current.toString().trim())
                    current = StringBuilder()
                } else {
                    current.append(char)
                }
            }
            else -> current.append(char)
        }
    }

    if (current.isNotEmpty()) {
        result.add(current.toString().trim())
    }

    return result
}

/**
 * Extracts just the type parameter name from a full declaration.
 * Handles variance modifiers (out/in) and constraints.
 *
 * Examples: "T" → "T", "out T" → "T", "T : Any" → "T"
 */
internal fun extractTypeParamName(param: String): String =
    param
        .substringBefore(" :")
        .removePrefix("out ")
        .removePrefix("in ")
        .trim()
