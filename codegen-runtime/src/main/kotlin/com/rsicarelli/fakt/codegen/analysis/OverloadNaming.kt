// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.analysis

/*
 * Every generated member of a fake (behavior property, DSL function, `*Calls` history, `*Call` data
 * class, verifier, `verify*` function) is derived from the function name. Overloads share that name,
 * so each overload gets a unique [FunctionSpec.name] built from its parameter types while
 * [FunctionSpec.sourceName] keeps the real name for the `override fun` and `super` calls:
 *
 * ```kotlin
 * fun find(): List<String>              // -> find          (keeps the plain name)
 * fun find(id: Int): String             // -> findInt
 * fun find(name: String): String        // -> findString
 * fun <T> set(feature: Feature, v: T?)  // -> setFeatureAny
 * ```
 */

/** Returns [this] declaration with overloaded functions renamed to unique member names. */
internal fun FakeDeclaration.withUniqueOverloadNames(): FakeDeclaration =
    when (this) {
        is FakeDeclaration.Interface -> {
            val renamed = functions.withUniqueOverloadNames(reserved(properties, typeParameters))
            copy(functions = renamed)
        }
        is FakeDeclaration.Class -> {
            val reserved = reserved(abstractProperties + openProperties, typeParameters)
            val renamed = (abstractMethods + openMethods).withUniqueOverloadNames(reserved)
            copy(
                abstractMethods = renamed.take(abstractMethods.size),
                openMethods = renamed.drop(abstractMethods.size),
            )
        }
    }

private fun reserved(properties: List<PropertySpec>, classTypeParameters: List<String>) =
    Reserved(
        names = properties.map { it.name }.toSet(),
        classTypeParameters = classTypeParameters.map { it.substringBefore(" :").trim() }.toSet(),
    )

private data class Reserved(val names: Set<String>, val classTypeParameters: Set<String>)

/**
 * Renames overloaded functions to `name + ParameterTypeNames`; non-overloaded functions (and a
 * parameterless overload) keep their name. Clashes left over are resolved with a numeric suffix.
 */
private fun List<FunctionSpec>.withUniqueOverloadNames(reserved: Reserved): List<FunctionSpec> {
    val overloadedNames = groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
    if (overloadedNames.isEmpty()) return this

    val taken =
        (map { it.name }.filterNot { it in overloadedNames } + reserved.names).toMutableSet()
    return map { function ->
        if (function.name !in overloadedNames) return@map function
        val candidate = function.name + function.overloadSuffix(reserved.classTypeParameters)
        val unique =
            generateSequence(2) { it + 1 }
                .map { "$candidate$it" }
                .let { sequenceOf(candidate) + it }
                .first { it !in taken }
        taken += unique
        function.copy(name = unique)
    }
}

/** Concatenated simple names of the receiver and parameter types (e.g. `IntString`). */
private fun FunctionSpec.overloadSuffix(classTypeParameters: Set<String>): String {
    val typeParameterNames =
        classTypeParameters + typeParameters.map { it.substringBefore(" :").trim() }
    val receiver =
        listOfNotNull(extensionReceiverTypeString).map { simpleTypeName(it, typeParameterNames) }
    val params =
        parameters.map { param ->
            val name =
                simpleTypeName(param.typeString.unwrapVararg(param.isVararg), typeParameterNames)
            if (param.isVararg) "Vararg$name" else name
        }
    return (receiver + params).joinToString("")
}

private fun String.unwrapVararg(isVararg: Boolean): String =
    if (isVararg && startsWith("Array<")) {
        removePrefix("Array<").removeSuffix(">").removePrefix("out ").trim()
    } else {
        this
    }

/**
 * Simple, capitalized name of a rendered type: generics, nullability and package are dropped,
 * function types become `Function` and type parameters become `Any`.
 *
 * Examples: `kotlin.collections.List<User>?` -> `List`, `(Int) -> Unit` -> `Function`, `T?` ->
 * `Any`
 */
private fun simpleTypeName(type: String, typeParameterNames: Set<String>): String {
    val trimmed = type.trim().removePrefix("suspend ").trim()
    if (trimmed.startsWith("(") || "->" in trimmed) return "Function"
    val simpleName =
        trimmed.substringBefore('<').removeSuffix("?").substringAfterLast('.').trim('`', ' ')
    val name = if (simpleName in typeParameterNames) "Any" else simpleName
    return name.filter { it.isLetterOrDigit() || it == '_' }.replaceFirstChar { it.uppercase() }
}
