// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.rendering

import com.rsicarelli.fakt.codegen.analysis.DeclarationAnnotation
import com.rsicarelli.fakt.compiler.fir.metadata.FirAnnotationArgument
import com.rsicarelli.fakt.compiler.fir.metadata.FirAnnotationInfo

/*
 * FIR annotation → DeclarationAnnotation rendering.
 *
 * Pure string transformation over the FIR-extracted annotation model — no compiler types. Shared
 * by both emission paths (the IR-phase FirToIrTransformer and the FIR-phase translator) so the
 * argument-rendering rules (escaping, vararg expansion, nested annotations) cannot drift between
 * phases.
 */

/**
 * Renders a [FirAnnotationInfo] to the pure [DeclarationAnnotation] consumed by codegen-runtime.
 *
 * Examples:
 * - `@OptIn(ExperimentalApi::class)` → `DeclarationAnnotation("OptIn", "kotlin.OptIn",
 *   ["ExperimentalApi::class"], isOptInMarker = …)`
 */
internal fun FirAnnotationInfo.toDeclarationAnnotation(): DeclarationAnnotation {
    val classIdParts = annotationClassId.split("/")
    return DeclarationAnnotation(
        simpleName = classIdParts.last().substringAfterLast('.'),
        fullyQualifiedName = annotationClassId.replace('/', '.'),
        renderedArguments = renderAnnotationArguments(arguments),
        isOptInMarker = isOptInMarker,
    )
}

/**
 * Renders annotation arguments to Kotlin source code strings.
 *
 * Rendering rules:
 * - Strings: `"value"` (escaped); numbers/booleans literally; chars quoted
 * - Class references: `Foo::class`; enums: `EnumClass.ENTRY`
 * - Named arrays: `[a, b]`; vararg-like arrays expanded to separate arguments
 * - Nested annotations: `@Nested(...)`
 */
internal fun renderAnnotationArguments(
    arguments: Map<String, FirAnnotationArgument>
): List<String> =
    arguments.flatMap { (name, value) ->
        // Vararg-like arrays (all class refs or all string literals — @OptIn, @Suppress, @Tags)
        // expand into separate arguments
        val isVarargLikeArray =
            value is FirAnnotationArgument.ArrayValue &&
                value.elements.isNotEmpty() &&
                (value.elements.all { it is FirAnnotationArgument.ClassReference } ||
                    value.elements.all { it is FirAnnotationArgument.StringLiteral })

        when {
            isVarargLikeArray -> {
                val arrayValue = value as FirAnnotationArgument.ArrayValue
                arrayValue.elements.map { renderArgumentValue(it) }
            }
            // Single argument named "value": skip the name
            arguments.size == 1 && name == "value" -> listOf(renderArgumentValue(value))
            // Single class reference (common vararg pattern): skip the name
            arguments.size == 1 && value is FirAnnotationArgument.ClassReference ->
                listOf(renderArgumentValue(value))
            else -> listOf("$name = ${renderArgumentValue(value)}")
        }
    }

private fun renderArgumentValue(value: FirAnnotationArgument): String =
    when (value) {
        is FirAnnotationArgument.ClassReference -> {
            val simpleName = value.classId.substringAfterLast('/').substringAfterLast('.')
            "$simpleName::class"
        }

        is FirAnnotationArgument.StringLiteral -> "\"${escapeString(value.value)}\""

        is FirAnnotationArgument.NumberLiteral -> value.value

        is FirAnnotationArgument.BooleanLiteral -> value.value.toString()

        is FirAnnotationArgument.CharLiteral -> "'${value.value}'"

        is FirAnnotationArgument.EnumValue -> {
            // For nested enums like LogLevel.Level, keep the full path after the package
            val enumClassPath = value.enumClassId.substringAfterLast('/')
            "$enumClassPath.${value.entryName}"
        }

        is FirAnnotationArgument.ArrayValue -> {
            // Vararg-like arrays are expanded by renderAnnotationArguments(); this branch handles
            // empty arrays and named array parameters
            if (value.elements.isEmpty()) {
                "emptyArray()"
            } else {
                val elementsStr = value.elements.joinToString(", ") { renderArgumentValue(it) }
                "[$elementsStr]"
            }
        }

        is FirAnnotationArgument.NestedAnnotation -> {
            val nested = value.annotation
            val simpleName =
                nested.annotationClassId.substringAfterLast('/').substringAfterLast('.')
            val argsStr =
                if (nested.arguments.isEmpty()) ""
                else "(${renderAnnotationArguments(nested.arguments).joinToString(", ")})"
            "@$simpleName$argsStr"
        }
    }

/** Escape special characters in string literals for code generation. */
private fun escapeString(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\t", "\\t")
        .replace("\r", "\\r")
