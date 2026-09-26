// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

/**
 * Stdlib generic types whose type parameters have no upper bound.
 *
 * `Any?` is always a valid type argument for these, so bounded type parameters keep erasing to
 * `Any?` there (star projections would break inference of defaults such as `emptyMap()`).
 */
private val unboundedStdlibGenericTypes =
    setOf(
        "Array",
        "Collection",
        "Flow",
        "Iterable",
        "List",
        "Map",
        "MutableCollection",
        "MutableIterable",
        "MutableList",
        "MutableMap",
        "MutableSet",
        "MutableSharedFlow",
        "MutableStateFlow",
        "Pair",
        "Result",
        "Sequence",
        "Set",
        "SharedFlow",
        "StateFlow",
        "Triple",
    )

private val varianceModifiers = listOf("out ", "in ")

/**
 * Returns the names of type parameters declared with an upper bound other than `Any?`.
 *
 * Examples: ["T : Enum<T>", "R", "out K : Any", "V : Any?"] -> {"T", "K"}
 *
 * @param declarations Type parameter declarations (e.g., ["T", "R : Comparable<R>"])
 * @return Names of the bounded type parameters
 */
internal fun boundedTypeParamNames(declarations: List<String>): Set<String> =
    declarations
        .filter { " :" in it && it.substringAfter(" :").trim() != "Any?" }
        .map(::extractTypeParamName)
        .toSet()

/**
 * Replaces type parameters that appear directly as a type argument with a star projection.
 *
 * Erasing a bounded type parameter to `Any?` inside a generic type whose own parameter is bounded
 * does not compile (`ABTest<Any?>` for `class ABTest<T : Enum<T>>`), while `*` always does.
 *
 * Only whole type arguments are replaced (including variance and nullability). Function-type
 * parameters, top-level occurrences and arguments of [unboundedStdlibGenericTypes] are left
 * untouched:
 * - "ABTest<T>" -> "ABTest<*>"
 * - "Box<String, out T?>" -> "Box<String, *>"
 * - "List<ABTest<T>>" -> "List<ABTest<*>>"
 * - "Map<String, T>" -> unchanged
 * - "(T) -> Box<(T) -> Unit>" -> unchanged
 *
 * @param type The type string to transform
 * @param typeParams The type parameter names to star-project
 * @return The type string with matching type arguments replaced by `*`
 */
internal fun starProjectTypeArguments(type: String, typeParams: Set<String>): String {
    if (typeParams.none { typeContainsParam(type, it) }) return type

    val result = StringBuilder(type.length)
    // Generic type owning each open `<` (null for parentheses)
    val owners = ArrayDeque<String?>()
    var i = 0
    while (i < type.length) {
        val argumentEnd =
            if (isStarProjectableArgumentStart(type[i], owners, result)) {
                matchTypeParamArgument(type, i, typeParams)
            } else {
                null
            }
        if (argumentEnd != null) {
            result.append('*')
            i = argumentEnd
        } else {
            i = appendToken(type, i, result, owners)
        }
    }
    return result.toString()
}

/** True when [c] begins a type argument of a generic type that may declare bounds. */
private fun isStarProjectableArgumentStart(
    c: Char,
    owners: ArrayDeque<String?>,
    emitted: CharSequence,
): Boolean {
    val owner = owners.lastOrNull() ?: return false
    val last = emitted.lastOrNull { !it.isWhitespace() }
    return !c.isWhitespace() &&
        owner !in unboundedStdlibGenericTypes &&
        (last == '<' || last == ',')
}

/** Appends the token at [start] (`->` or a single char), tracking brackets; returns next index. */
private fun appendToken(
    type: String,
    start: Int,
    result: StringBuilder,
    owners: ArrayDeque<String?>,
): Int {
    if (type.startsWith("->", start)) {
        result.append("->")
        return start + 2
    }
    when (val c = type[start]) {
        '<' -> owners.addLast(precedingSimpleName(result))
        '(' -> owners.addLast(null)
        '>',
        ')' -> owners.removeLastOrNull()
        else -> Unit
    }
    result.append(type[start])
    return start + 1
}

/** Simple name of the (possibly qualified) type name emitted right before a `<`. */
private fun precedingSimpleName(emitted: CharSequence): String {
    var start = emitted.length
    while (start > 0 && isIdentifierChar(emitted[start - 1])) start--
    return emitted.substring(start, emitted.length)
}

/**
 * Matches `[out |in ]Name[?]` at [start] where `Name` is one of [typeParams] and the argument ends
 * right after it (next non-blank char is `,` or `>`).
 *
 * @return Index right after the matched argument, or null when it does not match
 */
private fun matchTypeParamArgument(type: String, start: Int, typeParams: Set<String>): Int? {
    val nameStart = skipWhitespace(type, skipVariance(type, start))
    var i = nameStart
    while (i < type.length && (isIdentifierChar(type[i]) || type[i] == '.')) i++
    if (type.substring(nameStart, i) !in typeParams) return null
    if (type.getOrNull(i) == '?') i++
    val terminator = type.getOrNull(skipWhitespace(type, i))
    return if (terminator == ',' || terminator == '>') i else null
}

private fun skipVariance(type: String, start: Int): Int =
    varianceModifiers.firstOrNull { type.startsWith(it, start) }?.let { start + it.length } ?: start

private fun skipWhitespace(type: String, start: Int): Int {
    var i = start
    while (i < type.length && type[i].isWhitespace()) i++
    return i
}

private fun isIdentifierChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
