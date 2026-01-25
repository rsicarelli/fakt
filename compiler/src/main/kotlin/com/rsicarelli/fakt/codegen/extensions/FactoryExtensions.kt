// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import com.rsicarelli.fakt.compiler.fir.metadata.toModifier

/**
 * Generates a factory function string for creating fake implementations.
 *
 * @param spec Configuration for the factory function
 * @param includeKDoc Whether to include KDoc documentation (default: true)
 * @return Generated factory function code with optional KDoc
 */
fun generateFactoryFunction(
    spec: FactoryFunctionSpec,
    includeKDoc: Boolean = true,
): String {
    val names = FactoryNames(spec.interfaceName, spec.typeParameters)
    val (headerParams, whereClause) = parseTypeParametersForFactory(spec.typeParameters)
    val propagatedAnnotations = spec.annotations.filterPropagatable()

    return buildString {
        if (includeKDoc) appendKDoc(spec, names)
        appendAnnotations(propagatedAnnotations)
        appendSignature(names, headerParams, spec.visibility)
        appendBody(names, whereClause)
    }
}

/**
 * Overload for common use cases without KDoc customization.
 */
fun generateFactoryFunction(spec: FactoryFunctionSpec): String = generateFactoryFunction(spec, includeKDoc = true)

private data class FactoryNames(
    val interfaceName: String,
    val typeParameters: List<String>,
) {
    val fakeClassName = "Fake${interfaceName}Impl"
    val configClassName = "Fake${interfaceName}Config"
    val factoryName = "fake$interfaceName"
    val typeParamNames = typeParameters.map { it.substringBefore(" :").trim() }
    val typeArgs = if (typeParamNames.isNotEmpty()) "<${typeParamNames.joinToString(", ")}>" else ""
    val hasGenerics = typeParameters.isNotEmpty()
}

private fun List<AnnotationSpec>.filterPropagatable() =
    filter {
        (it.simpleName == "OptIn" || it.simpleName == "Deprecated") && !it.isOptInMarker
    }

private fun StringBuilder.appendKDoc(
    spec: FactoryFunctionSpec,
    names: FactoryNames,
) {
    val kdoc =
        KDocGenerator.generateFactoryKDoc(
            interfaceName = spec.interfaceName,
            factoryName = names.factoryName,
            implClassName = names.fakeClassName,
            methods = spec.methods,
            properties = spec.properties,
        )
    appendLine(kdoc)
}

private fun StringBuilder.appendAnnotations(annotations: List<AnnotationSpec>) {
    annotations.forEach { annotation ->
        val argsStr = if (annotation.arguments.isEmpty()) "" else "(${annotation.arguments.joinToString(", ")})"
        appendLine("@${annotation.simpleName}$argsStr")
    }
}

private fun StringBuilder.appendSignature(
    names: FactoryNames,
    headerParams: List<String>,
    visibility: FirVisibility,
) {
    if (names.hasGenerics) {
        val typeParamsStr =
            headerParams.joinToString(", ") { param ->
                val parts = param.split(" : ")
                if (parts.size > 1) "reified ${parts[0]} : ${parts[1]}" else "reified $param"
            }
        append("${visibility.toModifier()}inline fun <$typeParamsStr> ${names.factoryName}")
    } else {
        append("${visibility.toModifier()}inline fun ${names.factoryName}")
    }
    append("(configure: ${names.configClassName}${names.typeArgs}.() -> Unit = {})")
    append(": ${names.fakeClassName}${names.typeArgs}")
}

private fun StringBuilder.appendBody(
    names: FactoryNames,
    whereClause: String,
) {
    if (whereClause.isNotEmpty()) append(" where $whereClause")
    appendLine(" =")
    val impl = "${names.fakeClassName}${names.typeArgs}()"
    val config = "${names.configClassName}${names.typeArgs}(this)"
    append("    $impl.apply { $config.configure() }")
}

/**
 * Parses type parameters for factory function generation.
 *
 * Handles where clauses for multiple constraints on the same type parameter.
 *
 * @param typeParameters List of type parameter strings (e.g., ["T : Comparable<T>, Serializable"])
 * @return Pair of (header parameters, where clause string)
 */
private fun parseTypeParametersForFactory(typeParameters: List<String>): Pair<List<String>, String> {
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
            constraintList.forEach { constraint ->
                whereClauses.add("$name : $constraint")
            }
        }
    }

    return headerParams to whereClauses.joinToString(", ")
}
