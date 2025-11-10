// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

/**
 * Generates a factory function string for creating fake implementations.
 *
 * Creates a top-level function that instantiates the fake implementation
 * with optional configuration via DSL.
 *
 * Example output:
 * ```kotlin
 * fun fakeUserService(configure: FakeUserServiceConfig.() -> Unit = {}): FakeUserServiceImpl {
 *     return FakeUserServiceImpl().apply { FakeUserServiceConfig(this).configure() }
 * }
 * ```
 *
 * Note: Currently generates as string because the DSL doesn't support
 * function-level type parameters with reified modifier and where clauses yet.
 * This will be refactored when DSL gains full support.
 *
 * @param interfaceName The name of the interface being faked
 * @param typeParameters List of type parameters with constraints (e.g., ["T : Comparable<T>"])
 * @return Generated factory function code
 */
fun generateFactoryFunction(
    interfaceName: String,
    typeParameters: List<String> = emptyList(),
): String {
    val fakeClassName = "Fake${interfaceName}Impl"
    val configClassName = "Fake${interfaceName}Config"
    val factoryName = "fake$interfaceName"

    val hasGenerics = typeParameters.isNotEmpty()

    // Extract type parameter names (without constraints)
    val typeParamNames = typeParameters.map { it.substringBefore(" :").trim() }

    // Build type arguments string for usage
    val typeArgs = if (typeParamNames.isNotEmpty()) {
        "<${typeParamNames.joinToString(", ")}>"
    } else {
        ""
    }

    // Parse type parameters into header format and where clause
    val (headerParams, whereClause) = parseTypeParametersForFactory(typeParameters)

    return buildString {
        // Function signature
        if (hasGenerics) {
            // inline fun <reified T : Bound> fakeInterface(...)
            val typeParamsStr = headerParams.joinToString(", ") { param ->
                val parts = param.split(" : ")
                if (parts.size > 1) {
                    "reified ${parts[0]} : ${parts[1]}"
                } else {
                    "reified $param"
                }
            }
            append("inline fun <$typeParamsStr> $factoryName")
        } else {
            append("fun $factoryName")
        }

        // Parameters
        append("(configure: $configClassName$typeArgs.() -> Unit = {})")

        // Return type
        append(": $fakeClassName$typeArgs")

        // Where clause
        if (whereClause.isNotEmpty()) {
            append(" where $whereClause")
        }

        appendLine(" {")

        // Body
        appendLine("    return $fakeClassName$typeArgs().apply { $configClassName$typeArgs(this).configure() }")
        append("}")
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
