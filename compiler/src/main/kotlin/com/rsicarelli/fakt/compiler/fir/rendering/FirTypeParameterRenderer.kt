// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.rendering

import com.rsicarelli.fakt.compiler.fir.metadata.FirTypeParameterInfo

/*
 * FIR type-parameter → declaration-string rendering.
 *
 * Pure string transformation shared by both emission paths (FirToIrTransformer and the FIR-phase
 * translator) so the bound-sanitization rules cannot drift between phases.
 */

/**
 * Formats a type parameter with bounds for code generation:
 * - `T` with no bounds → `"T"`
 * - `K` with bounds `["kotlin/Comparable<K>"]` → `"K : Comparable<K>"`
 * - `V` with bounds `["kotlin/Comparable<V>", "Serializable"]` → `"V : Comparable<V>,
 *   Serializable"`
 */
internal fun formatTypeParameter(firTypeParam: FirTypeParameterInfo): String {
    if (firTypeParam.bounds.isEmpty()) {
        return firTypeParam.name
    }
    val sanitizedBounds = firTypeParam.bounds.map { bound -> sanitizeTypeBound(bound) }
    return "${firTypeParam.name} : ${sanitizedBounds.joinToString(", ")}"
}

/**
 * Sanitizes a bound string captured via `ConeKotlinType.toString()` into valid Kotlin syntax:
 * slashes become dots (`kotlin/Any?` → `kotlin.Any?`) and `kotlin.`/`kotlin.collections.` prefixes
 * are stripped (stdlib types are auto-imported). Other packages are preserved.
 */
internal fun sanitizeTypeBound(bound: String): String {
    val dotted = bound.replace('/', '.')

    return when {
        dotted.startsWith("kotlin.collections.") -> dotted.removePrefix("kotlin.collections.")
        dotted.startsWith("kotlin.") -> dotted.removePrefix("kotlin.")
        else -> dotted
    }
}
