// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.codegen.builder.FunctionBuilder

/**
 * Creates an override method that delegates to a behavior property.
 *
 * Generates pattern:
 * ```kotlin
 * override fun <T> methodName(params): ReturnType {
 *     return methodNameBehavior(params)  // For interfaces/abstract methods
 *     // OR
 *     return methodNameBehavior?.invoke(params) ?: super.methodName(params)  // For open methods
 * }
 * ```
 *
 * @param name Method name
 * @param params List of (name, type, isVararg) triples for parameters
 * @param returnType Return type
 * @param isSuspend Whether this is a suspend function
 * @param typeParameters Method-level type parameters (e.g., ["T", "R : Comparable<R>"])
 * @param useSuperDelegation If true, generates nullable invoke with super delegation for open methods
 * @param extensionReceiverType Extension receiver type (e.g., "Vector" for fun Vector.plus())
 * @param isOperator Whether this is an operator function
 */
fun ClassBuilder.overrideMethod(
    name: String,
    params: List<Triple<String, String, Boolean>>,
    returnType: String,
    isSuspend: Boolean = false,
    typeParameters: List<String> = emptyList(),
    useSuperDelegation: Boolean = false,
    extensionReceiverType: String? = null,
    isOperator: Boolean = false,
) {
    function(name) {
        if (isOperator) operator()
        if (extensionReceiverType != null) receiver(extensionReceiverType)
        override()
        if (isSuspend) suspend()

        // Add method-level type parameters
        typeParameters.forEach { typeParam ->
            // Parse "T" or "T : Bound"
            val parts = typeParam.split(" : ", limit = 2)
            val name = parts[0].trim()
            val constraints = if (parts.size > 1) arrayOf(parts[1].trim()) else emptyArray()
            typeParam(name, *constraints)
        }

        params.forEach { (paramName, paramType, isVararg) ->
            if (isVararg) {
                // Extract element type from Array<T>
                val elementType = paramType
                    .removePrefix("Array<")
                    .removeSuffix(">")
                    .removePrefix("out ")
                    .trim()
                parameter(paramName, elementType, vararg = true)
            } else {
                parameter(paramName, paramType)
            }
        }

        returns(returnType)

        val callTracking = "_${name}CallCount.update { it + 1 }"

        // When method has type parameters, we need to cast parameters to erased types
        val needsCast = typeParameters.isNotEmpty()

        // Generate parameter names for behavior invocation
        val regularParamNames = if (needsCast) {
            // Extract type parameter names for erasure checking
            val typeParamNames = typeParameters.map { it.split(" : ", limit = 2)[0].trim() }.toSet()

            params.joinToString(", ") { (paramName, paramType, _) ->
                // Check if this parameter type contains method-level generic types
                val containsMethodGeneric = typeParamNames.any { typeParam ->
                    paramType.contains(Regex("\\b$typeParam\\b"))
                }

                if (containsMethodGeneric) {
                    // Erase the parameter type
                    var erasedType = paramType
                    typeParamNames.forEach { typeParam ->
                        erasedType = erasedType.replace(Regex("\\b$typeParam\\b"), "Any?")
                    }
                    "$paramName as $erasedType"
                } else {
                    paramName
                }
            }
        } else {
            params.joinToString(", ") { it.first }
        }

        // For extension functions, prepend 'this' receiver as first argument
        val paramNames = if (extensionReceiverType != null) {
            if (regularParamNames.isEmpty()) {
                "this"
            } else {
                "this, $regularParamNames"
            }
        } else {
            regularParamNames
        }

        val returnCast = if (needsCast && returnType != "Unit") " as $returnType" else ""

        // Generate super call parameters (handle varargs and named parameters after varargs)
        val hasVararg = params.any { it.third }
        val varargIndex = if (hasVararg) params.indexOfFirst { it.third } else -1
        val superCallParams = params.mapIndexed { index, (paramName, _, isVararg) ->
            when {
                isVararg -> "*$paramName"
                hasVararg && index > varargIndex -> "$paramName = $paramName"  // Named parameter after vararg
                else -> paramName
            }
        }.joinToString(", ")

        // Generate body with super delegation for open methods
        body = if (useSuperDelegation) {
            // Open method: nullable invoke with super delegation
            val invocation = "${name}Behavior?.invoke($paramNames)"
            val superCall = "super.$name($superCallParams)"

            if (returnType == "Unit") {
                "$callTracking\n        $invocation ?: $superCall"
            } else {
                if (needsCast) {
                    "$callTracking\n        @Suppress(\"UNCHECKED_CAST\")\n        return ($invocation ?: $superCall)$returnCast"
                } else {
                    "$callTracking\n        return $invocation ?: $superCall"
                }
            }
        } else {
            // Abstract or interface method: direct behavior call
            if (returnType == "Unit") {
                "$callTracking\n        ${name}Behavior($paramNames)"
            } else {
                if (needsCast) {
                    "$callTracking\n        @Suppress(\"UNCHECKED_CAST\")\n        return ${name}Behavior($paramNames)$returnCast"
                } else {
                    "$callTracking\n        return ${name}Behavior($paramNames)"
                }
            }
        }
    }
}

/**
 * Creates an override method with vararg parameter.
 *
 * Generates pattern:
 * ```kotlin
 * override fun methodName(vararg items: T): ReturnType {
 *     return methodNameBehavior(items)  // For interfaces/abstract methods
 *     // OR
 *     return methodNameBehavior?.invoke(items) ?: super.methodName(*items)  // For open methods
 * }
 * ```
 *
 * @param varargType The Array type (e.g., "Array<String>"), element type will be extracted
 * @param useSuperDelegation If true, generates nullable invoke with super delegation for open methods
 */
fun ClassBuilder.overrideVarargMethod(
    name: String,
    varargName: String,
    varargType: String,
    returnType: String,
    useSuperDelegation: Boolean = false,
    extensionReceiverType: String? = null,
    isOperator: Boolean = false,
) {
    function(name) {
        if (isOperator) operator()
        if (extensionReceiverType != null) receiver(extensionReceiverType)
        override()
        // Extract element type from Array<T> or Array<out T>
        // "Array<String>" -> "String"
        // "Array<out String>" -> "String"
        val elementType = varargType
            .removePrefix("Array<")
            .removeSuffix(">")
            .removePrefix("out ")
            .trim()

        parameter(varargName, elementType, vararg = true)
        returns(returnType)

        val callTracking = "_${name}CallCount.update { it + 1 }"

        // For extension functions, prepend 'this' receiver as first argument
        val paramNames = if (extensionReceiverType != null) {
            "this, $varargName"
        } else {
            varargName
        }

        body = if (useSuperDelegation) {
            // Open method: nullable invoke with super delegation
            val invocation = "${name}Behavior?.invoke($paramNames)"
            val superCall = "super.$name(*$varargName)"

            if (returnType == "Unit") {
                "$callTracking\n        $invocation ?: $superCall"
            } else {
                "$callTracking\n        return $invocation ?: $superCall"
            }
        } else {
            // Abstract or interface method: direct behavior call
            if (returnType == "Unit") {
                "$callTracking\n        ${name}Behavior($paramNames)"
            } else {
                "$callTracking\n        return ${name}Behavior($paramNames)"
            }
        }
    }
}

/**
 * Creates a configuration method for behavior.
 *
 * Generates pattern:
 * ```kotlin
 * internal fun <T> configure{MethodName}(behavior: (Params) -> ReturnType) {
 *     {methodName}Behavior = behavior
 * }
 * ```
 *
 * @param methodName Method name
 * @param paramTypes Parameter types
 * @param returnType Return type
 * @param isSuspend Whether this is a suspend function
 * @param typeParameters Method-level type parameters (e.g., ["T", "R : Comparable<R>"])
 */
fun ClassBuilder.configureMethod(
    methodName: String,
    paramTypes: List<String>,
    returnType: String,
    isSuspend: Boolean = false,
    typeParameters: List<String> = emptyList(),
) {
    val capitalizedName = methodName.replaceFirstChar { it.uppercase() }

    val functionType = buildString {
        if (isSuspend) append("suspend ")
        append("(")
        append(paramTypes.joinToString(", "))
        append(") -> ")
        append(returnType)
    }

    function("configure$capitalizedName") {
        internal()

        // Add method-level type parameters
        typeParameters.forEach { typeParam ->
            val parts = typeParam.split(" : ", limit = 2)
            val name = parts[0].trim()
            val constraints = if (parts.size > 1) arrayOf(parts[1].trim()) else emptyArray()
            typeParam(name, *constraints)
        }

        parameter("behavior", functionType)
        returns("Unit")

        // Add cast when method has type parameters (behavior property uses erased types)
        val needsCast = typeParameters.isNotEmpty()
        body = if (needsCast) {
            // Build erased function type for cast by erasing method-level type parameters
            // Apply the same erasure logic used in generateMethod (FakeGenerator.kt)
            val erasedParams = paramTypes.map { paramType ->
                // Use the type erasure helper to properly erase nested generic types
                var erased = paramType
                typeParameters.forEach { typeParam ->
                    val paramName = typeParam.split(" : ", limit = 2)[0].trim()
                    erased = erased.replace(Regex("\\b$paramName\\b"), "Any?")
                }
                erased
            }
            val erasedReturn = run {
                var erased = returnType
                typeParameters.forEach { typeParam ->
                    val paramName = typeParam.split(" : ", limit = 2)[0].trim()
                    erased = erased.replace(Regex("\\b$paramName\\b"), "Any?")
                }
                erased
            }

            val erasedFunctionType = buildString {
                if (isSuspend) append("suspend ")
                append("(")
                append(erasedParams.joinToString(", "))
                append(") -> ")
                append(erasedReturn)
            }
            "@Suppress(\"UNCHECKED_CAST\")\n        ${methodName}Behavior = behavior as $erasedFunctionType"
        } else {
            "${methodName}Behavior = behavior"
        }
    }
}

/**
 * Configures this function as an override that delegates to behavior.
 *
 * Extension for FunctionBuilder to create delegation pattern.
 */
fun FunctionBuilder.delegateToBehavior(functionName: String, parameterNames: List<String>) {
    val invocation = "$functionName(${parameterNames.joinToString(", ")})"
    body = "return $invocation"
}

/**
 * Configures this function as a simple override with inline body.
 */
fun FunctionBuilder.asSimpleOverride(bodyExpression: String) {
    override()
    body = bodyExpression
}

/**
 * Creates an override property getter that delegates to a StateFlow.
 *
 * Used for StateFlow properties that delegate to backing MutableStateFlow.
 */
fun ClassBuilder.overridePropertyGetter(
    name: String,
    type: String,
    backingPropertyName: String,
) {
    property(name, type) {
        override()
        getter = backingPropertyName
    }
}
