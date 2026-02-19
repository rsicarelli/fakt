// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.model.CodeFile
import com.rsicarelli.fakt.codegen.model.CodeFunction
import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.renderTo
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Generates a factory function CodeFile for creating fake implementations.
 *
 * Uses type-safe DSL for clean, composable code generation. The returned CodeFile contains only the
 * function declaration (no package/imports), suitable for being added to an existing file.
 *
 * @param spec Configuration for the factory function
 * @param includeKDoc Whether to include KDoc documentation (default: true)
 * @return CodeFile containing the factory function
 */
fun generateFactoryFunctionCodeFile(
    spec: FactoryFunctionSpec,
    includeKDoc: Boolean = true,
): CodeFile {
    val names = FactoryNames(spec.interfaceName, spec.typeParameters)
    val (headerParams, whereClause) = parseTypeParametersForFactory(spec.typeParameters)
    val propagatedAnnotations = spec.annotations.filterPropagatable()

    return codeFile("") {
        function(names.factoryName) {
            // Apply visibility
            when (spec.visibility) {
                FirVisibility.PUBLIC -> public()
                FirVisibility.INTERNAL -> internal()
                else -> public()
            }

            isInline = true

            // Add type parameters with reified
            headerParams.forEach { param ->
                val parts = param.split(" : ", limit = 2)
                val name = parts[0].trim()
                val constraint = if (parts.size > 1) parts[1].trim() else null
                if (constraint != null) {
                    typeParam(name, constraint, reified = true)
                } else {
                    typeParam(name, reified = true)
                }
            }

            // Add where clause if needed
            if (whereClause.isNotEmpty()) {
                where(whereClause)
            }

            // Add configure parameter
            parameter(
                name = "configure",
                type = "${names.configClassName}${names.typeArgs}.() -> Unit",
                defaultValue = "{}",
            )

            // Set return type
            returns("${names.fakeClassName}${names.typeArgs}")

            // Delegate to config.build() — avoids inline/internal visibility conflict
            expressionBody = "${names.configClassName}${names.typeArgs}().apply(configure).build()"

            // Add KDoc if requested
            if (includeKDoc) {
                kdoc = generateFactoryKDocContent(spec, names)
            }

            // Add propagated annotations
            propagatedAnnotations.forEach { annotation ->
                annotation(annotation.simpleName, annotation.arguments)
            }
        }
    }
}

/**
 * Generates a factory function string for creating fake implementations.
 *
 * This delegates to the DSL-based implementation and renders the result to a string.
 *
 * @param spec Configuration for the factory function
 * @param includeKDoc Whether to include KDoc documentation (default: true)
 * @return Generated factory function code with optional KDoc
 */
fun generateFactoryFunction(spec: FactoryFunctionSpec, includeKDoc: Boolean = true): String {
    val codeFile = generateFactoryFunctionCodeFile(spec, includeKDoc)

    // Extract the function declaration and render it
    val function = codeFile.declarations.firstOrNull() as? CodeFunction ?: return ""

    val builder = CodeBuilder()
    function.renderTo(builder)
    return builder.build().trimEnd()
}

/** Overload for common use cases without KDoc customization. */
fun generateFactoryFunction(spec: FactoryFunctionSpec): String =
    generateFactoryFunction(spec, includeKDoc = true)

/** Generates KDoc content (without comment markers) for factory function. */
private fun generateFactoryKDocContent(spec: FactoryFunctionSpec, names: FactoryNames): String {
    val fullKDoc =
        KDocGenerator.generateFactoryKDoc(
            interfaceName = spec.interfaceName,
            factoryName = names.factoryName,
            implClassName = names.fakeClassName,
            methods = spec.methods,
            properties = spec.properties,
        )
    // Remove the /** and */ markers and leading " * " from each line
    return fullKDoc
        .lines()
        .drop(1) // Remove opening /**
        .dropLast(1) // Remove closing */
        .joinToString("\n") { line -> line.removePrefix(" * ").removePrefix(" *") }
        .trim()
}

private data class FactoryNames(val interfaceName: String, val typeParameters: List<String>) {
    val fakeClassName = "Fake${interfaceName}Impl"
    val configClassName = "Fake${interfaceName}Config"
    val factoryName = "fake$interfaceName"
    val typeParamNames = typeParameters.map { it.substringBefore(" :").trim() }
    val typeArgs = if (typeParamNames.isNotEmpty()) "<${typeParamNames.joinToString(", ")}>" else ""
    val hasGenerics = typeParameters.isNotEmpty()
}

private fun List<AnnotationSpec>.filterPropagatable() = filter {
    (it.simpleName == "OptIn" || it.simpleName == "Deprecated") && !it.isOptInMarker
}

/**
 * Parses type parameters for factory function generation.
 *
 * Handles where clauses for multiple constraints on the same type parameter.
 *
 * @param typeParameters List of type parameter strings (e.g., ["T : Comparable<T>, Serializable"])
 * @return Pair of (header parameters, where clause string)
 */
private fun parseTypeParametersForFactory(
    typeParameters: List<String>
): Pair<List<String>, String> {
    if (typeParameters.isEmpty()) {
        return emptyList<String>() to ""
    }

    val headerParams = mutableListOf<String>()
    val whereClauses = mutableListOf<String>()

    for (typeParam in typeParameters) {
        val colonIndex = typeParam.indexOf(" :")
        if (colonIndex == -1) {
            // No constraints
            headerParams.add(typeParam)
            continue
        }

        val name = typeParam.substring(0, colonIndex).trim()
        val constraints = typeParam.substring(colonIndex + 2).trim()
        val constraintList = constraints.split(",").map { it.trim() }

        if (constraintList.size == 1) {
            // Single constraint: keep in header
            headerParams.add(typeParam)
        } else {
            // Multiple constraints: use where clause
            headerParams.add(name)
            constraintList.forEach { constraint -> whereClauses.add("$name : $constraint") }
        }
    }

    return headerParams to whereClauses.joinToString(", ")
}
