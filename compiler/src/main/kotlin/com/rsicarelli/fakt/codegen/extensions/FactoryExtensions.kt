// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import com.rsicarelli.fakt.compiler.fir.metadata.toModifier

/**
 * Generates a factory function string for creating fake implementations.
 *
 * Creates a top-level function that instantiates the fake implementation
 * with optional configuration via DSL. Optionally includes KDoc documentation
 * that appears in IDE autocomplete.
 *
 * Example output (with KDoc):
 * ```kotlin
 * /**
 *  * Creates a fake implementation of [UserService] for testing.
 *  *
 *  * Example:
 *  * ```kotlin
 *  * val fake = fakeUserService {
 *  *     getUser { userId -> User(id = userId, name = "Test") }
 *  * }
 *  * ```
 *  *
 *  * ## Configurable Behaviors
 *  * - `getUser`: (String) -> User?
 *  *
 *  * @param configure DSL block to configure fake behaviors
 *  * @return Configured [FakeUserServiceImpl] instance
 *  */
 * inline fun fakeUserService(configure: FakeUserServiceConfig.() -> Unit = {}): FakeUserServiceImpl =
 *     FakeUserServiceImpl().apply { FakeUserServiceConfig(this).configure() }
 * ```
 *
 * Note: Currently generates as string because the DSL doesn't support
 * function-level type parameters with reified modifier and where clauses yet.
 * This will be refactored when DSL gains full support.
 *
 * @param interfaceName The name of the interface being faked
 * @param typeParameters List of type parameters with constraints (e.g., ["T : Comparable<T>"])
 * @param visibility Visibility for the generated function (PUBLIC, INTERNAL) for explicitApi() support
 * @param annotations Annotations to propagate to the factory function (@OptIn, @Deprecated)
 * @param methods Optional list of methods for KDoc generation
 * @param properties Optional list of properties for KDoc generation
 * @param includeKDoc Whether to include KDoc documentation (default: true)
 * @return Generated factory function code with optional KDoc
 */
fun generateFactoryFunction(
    interfaceName: String,
    typeParameters: List<String> = emptyList(),
    visibility: FirVisibility = FirVisibility.PUBLIC,
    annotations: List<AnnotationSpec> = emptyList(),
    methods: List<MethodSpec> = emptyList(),
    properties: List<PropertySpec> = emptyList(),
    includeKDoc: Boolean = true,
): String {
    val fakeClassName = "Fake${interfaceName}Impl"
    val configClassName = "Fake${interfaceName}Config"
    val factoryName = "fake$interfaceName"

    val hasGenerics = typeParameters.isNotEmpty()

    // Extract type parameter names (without constraints)
    val typeParamNames = typeParameters.map { it.substringBefore(" :").trim() }

    // Build type arguments string for usage
    val typeArgs =
        if (typeParamNames.isNotEmpty()) {
            "<${typeParamNames.joinToString(", ")}>"
        } else {
            ""
        }

    // Parse type parameters into header format and where clause
    val (headerParams, whereClause) = parseTypeParametersForFactory(typeParameters)

    // Filter annotations to propagate (@OptIn, @Deprecated), excluding opt-in markers
    // We don't propagate opt-in markers because:
    // 1. The impl class already has @OptIn(MarkerClass::class)
    // 2. Factory just creates the impl, doesn't need its own @OptIn
    val propagatedAnnotations =
        annotations.filter {
            (it.simpleName == "OptIn" || it.simpleName == "Deprecated") && !it.isOptInMarker
        }

    return buildString {
        // Add KDoc if enabled and there's content to document
        if (includeKDoc) {
            val kdoc =
                KDocGenerator.generateFactoryKDoc(
                    interfaceName = interfaceName,
                    factoryName = factoryName,
                    implClassName = fakeClassName,
                    methods = methods,
                    properties = properties,
                )
            appendLine(kdoc)
        }

        // Add propagated annotations (@OptIn, @Deprecated)
        propagatedAnnotations.forEach { annotation ->
            val argsStr =
                if (annotation.arguments.isEmpty()) {
                    ""
                } else {
                    "(${annotation.arguments.joinToString(", ")})"
                }
            appendLine("@${annotation.simpleName}$argsStr")
        }

        // Function signature with visibility modifier
        if (hasGenerics) {
            // public inline fun <reified T : Bound> fakeInterface(...)
            val typeParamsStr =
                headerParams.joinToString(", ") { param ->
                    val parts = param.split(" : ")
                    if (parts.size > 1) {
                        "reified ${parts[0]} : ${parts[1]}"
                    } else {
                        "reified $param"
                    }
                }
            append("${visibility.toModifier()}inline fun <$typeParamsStr> $factoryName")
        } else {
            append("${visibility.toModifier()}inline fun $factoryName")
        }

        // Parameters
        append("(configure: $configClassName$typeArgs.() -> Unit = {})")

        // Return type
        append(": $fakeClassName$typeArgs")

        // Where clause
        if (whereClause.isNotEmpty()) {
            append(" where $whereClause")
        }

        appendLine(" =")

        // Body - expression syntax
        append("    $fakeClassName$typeArgs().apply { $configClassName$typeArgs(this).configure() }")
    }
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
