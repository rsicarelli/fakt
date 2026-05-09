// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder

/**
 * Builds the history update statement for recording a method call.
 *
 * For ALL methods:
 * - Methods with params: `_xxxCalls.update { it + XxxCall(params) }`
 * - 0-param/vararg-only methods: `_xxxCalls.update { it + Unit }`
 *
 * @param interfaceName Name of the interface for collision-safe data class naming
 * @param methodName Name of the method
 * @param params List of (name, type, isVararg) triples for parameters
 * @param classTypeParams Set of class-level type parameter names that need erasure casts
 * @param methodTypeParams Set of method-level type parameter names that need erasure casts
 * @return Update statement string for call history recording
 */
private fun buildHistoryUpdateStatement(
    interfaceName: String,
    methodName: String,
    params: List<Triple<String, String, Boolean>>,
    classTypeParams: Set<String> = emptySet(),
    methodTypeParams: Set<String> = emptySet(),
): String {
    val regularParams = params.filterNot { it.third } // Exclude varargs

    // 0-param or vararg-only methods use Unit
    if (regularParams.isEmpty()) {
        return "${methodName}Calls.update { it + Unit }"
    }

    val allTypeParams = classTypeParams + methodTypeParams

    // Interface-prefixed data class for collision safety
    val dataClassName = "${interfaceName}${methodName.replaceFirstChar { it.uppercase() }}Call"

    // Build constructor args, adding casts for parameters that contain type parameters
    val constructorArgs =
        regularParams.joinToString(", ") { (name, type, _) ->
            // Check if the parameter type contains any type parameter
            if (typeContainsAnyParam(type, allTypeParams)) {
                val erasedType = eraseTypeParamsToAny(type, allTypeParams)
                "$name as $erasedType"
            } else {
                name
            }
        }

    return "${methodName}Calls.update { it + $dataClassName($constructorArgs) }"
}

/**
 * Configuration for override method generation.
 *
 * @property generateCallHistory When true, generates call tracking code in the method body. When
 *   false, skips call tracking for lightweight fakes. Default: true.
 */
data class OverrideMethodConfig(
    val isSuspend: Boolean = false,
    val typeParameters: List<String> = emptyList(),
    val useSuperDelegation: Boolean = false,
    val extensionReceiverType: String? = null,
    val isOperator: Boolean = false,
    val interfaceName: String = "",
    val classTypeParameters: List<String> = emptyList(),
    val generateCallHistory: Boolean = true,
    val behaviorPrefix: String = "",
)

/** Creates an override method that delegates to a behavior property. */
fun ClassBuilder.overrideMethod(
    name: String,
    params: List<Triple<String, String, Boolean>>,
    returnType: String,
    config: OverrideMethodConfig = OverrideMethodConfig(),
) {
    val classTypeParamNames = config.classTypeParameters.map(::extractTypeParamName).toSet()
    val methodTypeParamNames = config.typeParameters.map(::extractTypeParamName).toSet()

    // Only method-level type params require unchecked casts — their types are erased to Any? in
    // the behavior constructor param. Class-level type params are preserved in the constructor,
    // so no cast is needed for behavior invocation or return.
    val needsCast = config.typeParameters.isNotEmpty()

    function(name) {
        if (needsCast) annotation("Suppress", "\"UNCHECKED_CAST\"")
        if (config.isOperator) operator()
        config.extensionReceiverType?.let { receiver(it) }
        override()
        if (config.isSuspend) suspend()

        config.typeParameters.forEach { typeParam ->
            val parts = typeParam.split(" : ", limit = 2)
            if (parts.size > 1) {
                typeParam(parts[0].trim(), parts[1].trim())
            } else {
                typeParam(parts[0].trim())
            }
        }

        params.forEach { (paramName, paramType, isVararg) ->
            if (isVararg) {
                val elementType =
                    paramType.removePrefix("Array<").removeSuffix(">").removePrefix("out ").trim()
                parameter(paramName, elementType, vararg = true)
            } else {
                parameter(paramName, paramType)
            }
        }
        returns(returnType)

        val callTracking =
            if (config.generateCallHistory) {
                buildHistoryUpdateStatement(
                    config.interfaceName,
                    name,
                    params,
                    classTypeParamNames,
                    methodTypeParamNames,
                )
            } else {
                null
            }
        val paramNames =
            buildBehaviorInvocationParams(
                params,
                config.extensionReceiverType,
                needsCast,
                methodTypeParamNames,
            )
        val returnCast = if (needsCast && returnType != "Unit") " as $returnType" else ""
        val superCallParams = buildSuperCallParams(params)

        val bp = config.behaviorPrefix
        body =
            if (config.useSuperDelegation) {
                val invocation = "$bp${name}Behavior?.invoke($paramNames)"
                val superCall = "super.$name($superCallParams)"
                if (returnType == "Unit") {
                    if (callTracking != null) {
                        "$callTracking\n$invocation ?: $superCall"
                    } else {
                        "$invocation ?: $superCall"
                    }
                } else {
                    if (callTracking != null) {
                        "$callTracking\nreturn ($invocation ?: $superCall)$returnCast"
                    } else {
                        "return ($invocation ?: $superCall)$returnCast"
                    }
                }
            } else {
                if (returnType == "Unit") {
                    if (callTracking != null) {
                        "$callTracking\n$bp${name}Behavior($paramNames)"
                    } else {
                        "$bp${name}Behavior($paramNames)"
                    }
                } else {
                    if (callTracking != null) {
                        "$callTracking\nreturn $bp${name}Behavior($paramNames)$returnCast"
                    } else {
                        "return $bp${name}Behavior($paramNames)$returnCast"
                    }
                }
            }
    }
}

private fun buildBehaviorInvocationParams(
    params: List<Triple<String, String, Boolean>>,
    extensionReceiverType: String?,
    needsCast: Boolean,
    methodTypeParamNames: Set<String>,
): String {
    val regularParamNames =
        if (needsCast) {
            params.joinToString(", ") { (paramName, paramType, _) ->
                if (typeContainsAnyParam(paramType, methodTypeParamNames)) {
                    "$paramName as ${eraseTypeParamsToAny(paramType, methodTypeParamNames)}"
                } else {
                    paramName
                }
            }
        } else {
            params.joinToString(", ") { it.first }
        }

    return when {
        extensionReceiverType == null -> regularParamNames
        regularParamNames.isEmpty() -> "this"
        else -> "this, $regularParamNames"
    }
}

private fun buildSuperCallParams(params: List<Triple<String, String, Boolean>>): String {
    val hasVararg = params.any { it.third }
    val varargIndex = if (hasVararg) params.indexOfFirst { it.third } else -1
    return params
        .mapIndexed { index, (paramName, _, isVararg) ->
            when {
                isVararg -> "*$paramName"
                hasVararg && index > varargIndex -> "$paramName = $paramName"
                else -> paramName
            }
        }
        .joinToString(", ")
}

/**
 * Configuration for override vararg method generation.
 *
 * @property useSuperDelegation If true, generates nullable invoke with super delegation for open
 *   methods
 * @property extensionReceiverType Extension receiver type for extension functions (e.g., "Vector")
 * @property isOperator Whether method is declared with 'operator' modifier
 * @property generateCallHistory When true, includes call tracking statement. Default: true.
 */
data class OverrideVarargConfig(
    val useSuperDelegation: Boolean = false,
    val extensionReceiverType: String? = null,
    val isOperator: Boolean = false,
    val generateCallHistory: Boolean = true,
    val behaviorPrefix: String = "",
)

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
 * @param config Configuration options for vararg method generation
 */
fun ClassBuilder.overrideVarargMethod(
    name: String,
    varargName: String,
    varargType: String,
    returnType: String,
    config: OverrideVarargConfig = OverrideVarargConfig(),
) {
    function(name) {
        if (config.isOperator) operator()
        if (config.extensionReceiverType != null) receiver(config.extensionReceiverType)
        override()
        // Extract element type from Array<T> or Array<out T>
        // "Array<String>" -> "String"
        // "Array<out String>" -> "String"
        val elementType =
            varargType.removePrefix("Array<").removeSuffix(">").removePrefix("out ").trim()

        parameter(varargName, elementType, vararg = true)
        returns(returnType)

        // Vararg-only methods use Unit for history (call count derived from history size)
        // Only generate if call history is enabled
        val callTracking =
            if (config.generateCallHistory) "${name}Calls.update { it + Unit }" else null

        // For extension functions, prepend 'this' receiver as first argument
        val paramNames =
            if (config.extensionReceiverType != null) {
                "this, $varargName"
            } else {
                varargName
            }

        val bp = config.behaviorPrefix
        body =
            if (config.useSuperDelegation) {
                // Open method: nullable invoke with super delegation
                val invocation = "$bp${name}Behavior?.invoke($paramNames)"
                val superCall = "super.$name(*$varargName)"

                if (returnType == "Unit") {
                    if (callTracking != null) {
                        "$callTracking\n$invocation ?: $superCall"
                    } else {
                        "$invocation ?: $superCall"
                    }
                } else {
                    if (callTracking != null) {
                        "$callTracking\nreturn $invocation ?: $superCall"
                    } else {
                        "return $invocation ?: $superCall"
                    }
                }
            } else {
                // Abstract or interface method: direct behavior call
                if (returnType == "Unit") {
                    if (callTracking != null) {
                        "$callTracking\n$bp${name}Behavior($paramNames)"
                    } else {
                        "$bp${name}Behavior($paramNames)"
                    }
                } else {
                    if (callTracking != null) {
                        "$callTracking\nreturn $bp${name}Behavior($paramNames)"
                    } else {
                        "return $bp${name}Behavior($paramNames)"
                    }
                }
            }
    }
}

/**
 * Creates a configuration method for behavior.
 *
 * Generates pattern:
 * ```kotlin
 * internal fun <T> configure{MethodName}(behavior: (Params) -> ReturnType) =
 *     run { {methodName}Behavior = behavior }
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

    // Add cast when method has type parameters (behavior property uses erased types)
    val needsCast = typeParameters.isNotEmpty()

    // Build erased function type for cast if needed
    val erasedFunctionType =
        if (needsCast) {
            val typeParamNames = typeParameters.map { it.split(" : ", limit = 2)[0].trim() }.toSet()
            val erasedParams = paramTypes.map { eraseTypeParamsSimple(it, typeParamNames) }
            val erasedReturn = eraseTypeParamsSimple(returnType, typeParamNames)
            buildString {
                if (isSuspend) append("suspend ")
                append("(")
                append(erasedParams.joinToString(", "))
                append(") -> ")
                append(erasedReturn)
            }
        } else {
            null
        }

    function("configure$capitalizedName") {
        // Add @Suppress annotation at function level when cast is needed
        if (needsCast) {
            annotation("Suppress", "\"UNCHECKED_CAST\"")
        }

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

        expressionBody =
            if (needsCast && erasedFunctionType != null) {
                "run { ${methodName}Behavior = behavior as $erasedFunctionType }"
            } else {
                "run { ${methodName}Behavior = behavior }"
            }
    }
}
