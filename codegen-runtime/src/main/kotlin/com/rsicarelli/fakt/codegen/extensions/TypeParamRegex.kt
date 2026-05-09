// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

/**
 * Cached regex patterns for type parameter matching.
 *
 * Regex compilation is expensive, so we cache patterns by parameter name. These are used throughout
 * code generation to detect and erase type parameters.
 */
private val typeParamWordBoundaryRegex = mutableMapOf<String, Regex>()
private val typeParamNonNullableRegex = mutableMapOf<String, Regex>()
private val typeParamNullableRegex = mutableMapOf<String, Regex>()

/**
 * Gets a cached regex that matches a type parameter as a whole word.
 *
 * Pattern: `\b{param}\b` - matches "T" but not "String" or "Test"
 *
 * @param param The type parameter name (e.g., "T", "K", "V")
 * @return Cached regex for word boundary matching
 */
internal fun getTypeParamWordBoundaryRegex(param: String): Regex =
    typeParamWordBoundaryRegex.getOrPut(param) { Regex("\\b$param\\b") }

/**
 * Gets a cached regex that matches a non-nullable type parameter.
 *
 * Pattern: `\b{param}\b(?!\?)` - matches "T" but not "T?"
 *
 * @param param The type parameter name
 * @return Cached regex for non-nullable matching
 */
internal fun getTypeParamNonNullableRegex(param: String): Regex =
    typeParamNonNullableRegex.getOrPut(param) { Regex("\\b$param\\b(?!\\?)") }

/**
 * Gets a cached regex that matches a nullable type parameter.
 *
 * Pattern: `\b{param}\?` - matches "T?" but not "T"
 *
 * @param param The type parameter name
 * @return Cached regex for nullable matching
 */
internal fun getTypeParamNullableRegex(param: String): Regex =
    typeParamNullableRegex.getOrPut(param) { Regex("\\b$param\\?") }

/**
 * Checks if a type string contains the given type parameter.
 *
 * Uses word boundary matching to avoid false positives like "T" in "String".
 *
 * @param type The type string to check
 * @param typeParam The type parameter name to look for
 * @return True if the type contains the type parameter
 */
internal fun typeContainsParam(type: String, typeParam: String): Boolean =
    type.contains(getTypeParamWordBoundaryRegex(typeParam))

/**
 * Checks if a type string contains any of the given type parameters.
 *
 * @param type The type string to check
 * @param typeParams The set of type parameter names to look for
 * @return True if the type contains any of the type parameters
 */
public fun typeContainsAnyParam(type: String, typeParams: Set<String>): Boolean =
    typeParams.any { typeContainsParam(type, it) }

/**
 * Erases type parameters in a type string to Any?.
 *
 * Handles both nullable and non-nullable occurrences:
 * - "T" -> "Any?"
 * - "T?" -> "Any?"
 * - "List<T>" -> "List<Any?>"
 * - "Map<K, V>" -> "Map<Any?, Any?>"
 *
 * @param type The type string to transform
 * @param typeParams The set of type parameter names to erase
 * @return The type string with all type parameters replaced by Any?
 */
public fun eraseTypeParamsToAny(type: String, typeParams: Set<String>): String {
    if (typeParams.isEmpty()) return type

    var result = type
    for (param in typeParams) {
        result =
            result
                .replace(getTypeParamNonNullableRegex(param), "Any?")
                .replace(getTypeParamNullableRegex(param), "Any?")
    }
    return result
}

/**
 * Erases type parameters (simple replacement without nullable handling).
 *
 * Used when we just need word boundary replacement without distinguishing between nullable and
 * non-nullable variants.
 *
 * @param type The type string to transform
 * @param typeParams The set of type parameter names to erase
 * @return The type string with all type parameters replaced by Any?
 */
internal fun eraseTypeParamsSimple(type: String, typeParams: Set<String>): String {
    if (typeParams.isEmpty()) return type

    var result = type
    for (param in typeParams) {
        result = result.replace(getTypeParamWordBoundaryRegex(param), "Any?")
    }
    return result
}
