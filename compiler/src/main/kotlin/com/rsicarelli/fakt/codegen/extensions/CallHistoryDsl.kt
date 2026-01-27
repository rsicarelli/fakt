// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.codegen.builder.CodeFileBuilder
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Generates a data class for recording method call arguments.
 *
 * Example output:
 * ```kotlin
 * public data class UserServiceGetUserCall(val id: String)
 * ```
 *
 * @param interfaceName Interface name for collision-safe prefix
 * @param methodName Method name
 * @param params List of (name, type, isVararg) triples
 * @param visibility Visibility modifier
 * @param allTypeParams Set of ALL type parameter names to erase (class-level + method-level)
 */
internal fun CodeFileBuilder.callDataClass(
    interfaceName: String,
    methodName: String,
    params: List<Triple<String, String, Boolean>>,
    visibility: FirVisibility,
    allTypeParams: Set<String>,
) {
    val regularParams = params.filterNot { it.third }
    if (regularParams.isEmpty()) return

    val className = "${interfaceName}${methodName.capitalizeFirst()}Call"

    dataClass(className) {
        visibility.applyVisibility(this)
        regularParams.forEach { (name, type, _) ->
            property(name, eraseTypeParamsToAny(type, allTypeParams))
        }
    }
}

/**
 * Generates a verifier class for methods with parameters.
 *
 * Example output:
 * ```kotlin
 * public class UserServiceGetUserCallVerifier(private val calls: List<UserServiceGetUserCall>) {
 *     public fun wasCalledTimes(n: Int): Boolean = calls.size == n
 *     public fun wasCalledWith(id: String): Boolean = calls.any { it.id == id }
 *     public fun wasNeverCalled(): Boolean = calls.isEmpty()
 *     public fun wasCalledInOrder(vararg ids: String): Boolean = calls.map { it.id } == ids.toList()
 *     public fun neverCalledWith(id: String): Boolean = calls.none { it.id == id }
 *     public val lastOrNull: UserServiceGetUserCall? get() = calls.lastOrNull()
 *     public val first: UserServiceGetUserCall get() = calls.first()
 *     public val all: List<UserServiceGetUserCall> get() = calls
 * }
 * ```
 *
 * @param interfaceName Interface name for collision-safe prefix
 * @param methodName Method name
 * @param params List of (name, type, isVararg) triples
 * @param visibility Visibility modifier
 * @param allTypeParams Set of ALL type parameter names to erase (class-level + method-level)
 */
internal fun CodeFileBuilder.verifierClass(
    interfaceName: String,
    methodName: String,
    params: List<Triple<String, String, Boolean>>,
    visibility: FirVisibility,
    allTypeParams: Set<String>,
) {
    val regularParams = params.filterNot { it.third }
    if (regularParams.isEmpty()) return

    val dataClassName = "${interfaceName}${methodName.capitalizeFirst()}Call"
    val verifierClassName = "${dataClassName}Verifier"

    // Erase ALL type parameters (class-level + method-level) to Any?
    val paramsWithErasedTypes =
        regularParams.map { (name, type, isVararg) ->
            Triple(name, eraseTypeParamsToAny(type, allTypeParams), isVararg)
        }

    klass(verifierClassName) {
        visibility.applyVisibility(this)
        constructorProperty("calls", "List<$dataClassName>") { private() }
        addCoreMethods(paramsWithErasedTypes, visibility)
        addSingleParamMethods(paramsWithErasedTypes, visibility)
        addAccessorProperties(dataClassName, visibility)
    }
}

private fun ClassBuilder.addCoreMethods(
    params: List<Triple<String, String, Boolean>>,
    visibility: FirVisibility,
) {
    function("wasCalledTimes") {
        visibility.applyVisibility(this)
        parameter("n", "Int")
        returns("Boolean")
        expressionBody = "calls.size == n"
    }
    val condition = params.joinToString(" && ") { (name, _, _) -> "it.$name == $name" }
    function("wasCalledWith") {
        visibility.applyVisibility(this)
        params.forEach { (name, type, _) -> parameter(name, type) }
        returns("Boolean")
        expressionBody = "calls.any { $condition }"
    }
    function("wasNeverCalled") {
        visibility.applyVisibility(this)
        returns("Boolean")
        expressionBody = "calls.isEmpty()"
    }
}

private fun ClassBuilder.addSingleParamMethods(
    params: List<Triple<String, String, Boolean>>,
    visibility: FirVisibility,
) {
    if (params.size != 1) return
    val (paramName, paramType, _) = params.first()
    // Skip vararg generation for Result types (Kotlin doesn't allow Result as vararg)
    if (!paramType.startsWith("Result<")) {
        function("wasCalledInOrder") {
            visibility.applyVisibility(this)
            parameter("${paramName}s", paramType, vararg = true)
            returns("Boolean")
            expressionBody = "calls.map { it.$paramName } == ${paramName}s.toList()"
        }
    }
    function("neverCalledWith") {
        visibility.applyVisibility(this)
        parameter(paramName, paramType)
        returns("Boolean")
        expressionBody = "calls.none { it.$paramName == $paramName }"
    }
}

private fun ClassBuilder.addAccessorProperties(
    dataClassName: String,
    visibility: FirVisibility,
) {
    property("lastOrNull", "$dataClassName?") {
        visibility.applyVisibility(this)
        getter = "calls.lastOrNull()"
    }
    property("first", dataClassName) {
        visibility.applyVisibility(this)
        getter = "calls.first()"
    }
    property("all", "List<$dataClassName>") {
        visibility.applyVisibility(this)
        getter = "calls"
    }
}

/**
 * Generates a simplified verifier class for 0-param/vararg-only methods.
 *
 * Example output:
 * ```kotlin
 * public class UserServiceLogoutCallVerifier(private val calls: List<Unit>) {
 *     public fun wasCalledTimes(n: Int): Boolean = calls.size == n
 *     public fun wasNeverCalled(): Boolean = calls.isEmpty()
 * }
 * ```
 *
 * @param interfaceName Interface name for collision-safe prefix
 * @param methodName Method name
 * @param visibility Visibility modifier
 */
internal fun CodeFileBuilder.unitVerifierClass(
    interfaceName: String,
    methodName: String,
    visibility: FirVisibility,
) {
    val verifierClassName = "${interfaceName}${methodName.capitalizeFirst()}CallVerifier"

    klass(verifierClassName) {
        visibility.applyVisibility(this)

        // Constructor property: private val calls: List<Unit>
        constructorProperty("calls", "List<Unit>") {
            private()
        }

        // fun wasCalledTimes(n: Int): Boolean = calls.size == n
        function("wasCalledTimes") {
            visibility.applyVisibility(this)
            parameter("n", "Int")
            returns("Boolean")
            expressionBody = "calls.size == n"
        }

        // fun wasNeverCalled(): Boolean = calls.isEmpty()
        function("wasNeverCalled") {
            visibility.applyVisibility(this)
            returns("Boolean")
            expressionBody = "calls.isEmpty()"
        }
    }
}

/**
 * Generates a verify extension function for methods with parameters.
 *
 * Example output (non-generic):
 * ```kotlin
 * /**
 *  * Verifies call history for [getUser] method.
 *  * ...
 *  */
 * public inline fun FakeUserServiceImpl.verifyGetUser(
 *     block: UserServiceGetUserCallVerifier.() -> Unit,
 * ): UserServiceGetUserCallVerifier = UserServiceGetUserCallVerifier(_getUserCalls.value).apply(block)
 * ```
 *
 * Example output (generic):
 * ```kotlin
 * public inline fun <T> FakeDataCacheImpl<T>.verifyStore(
 *     block: DataCacheStoreCallVerifier.() -> Unit,
 * ): DataCacheStoreCallVerifier = DataCacheStoreCallVerifier(_storeCalls.value).apply(block)
 * ```
 *
 * @param fakeClassName Name of the fake implementation class
 * @param interfaceName Interface name for collision-safe class references
 * @param methodName Method name
 * @param visibility Visibility modifier
 * @param classTypeParameters Class-level type parameters for generic extension functions
 */
internal fun CodeFileBuilder.verifyFunction(
    fakeClassName: String,
    interfaceName: String,
    methodName: String,
    visibility: FirVisibility,
    classTypeParameters: List<String>,
) {
    val capitalizedMethodName = methodName.capitalizeFirst()
    val dataClassName = "${interfaceName}${capitalizedMethodName}Call"
    val verifierClassName = "${dataClassName}Verifier"
    val backingFieldName = "_${methodName}Calls"
    val functionName = "verify$capitalizedMethodName"
    val typeParams = parseTypeParameters(classTypeParameters)

    // Build receiver type: FakeClassName or FakeClassName<T, K>
    val receiverType = "$fakeClassName${typeParams.usage}"

    function(functionName) {
        visibility.applyVisibility(this)
        isInline = true

        // Add type parameters without constraints (just names)
        // The constraints go in the where clause
        if (classTypeParameters.isNotEmpty()) {
            classTypeParameters.forEach { param ->
                val cleanParam = param.removePrefix("out ").removePrefix("in ").trim()
                val name = cleanParam.substringBefore(" :").trim()
                typeParam(name)
            }
        }

        // Add where clause if there are constraints
        if (typeParams.whereClause.isNotEmpty()) {
            // Remove the leading " where " prefix since we add it in the builder
            where(typeParams.whereClause.removePrefix(" where "))
        }

        receiver(receiverType)
        parameter("block", "$verifierClassName.() -> Unit")
        returns(verifierClassName)
        expressionBody = "$verifierClassName($backingFieldName.value).apply(block)"
    }
}

/**
 * Generates a verify extension function for 0-param/vararg-only methods.
 *
 * Uses simplified verifier with only wasCalledTimes and wasNeverCalled.
 *
 * @param fakeClassName Name of the fake implementation class
 * @param interfaceName Interface name for collision-safe class references
 * @param methodName Method name
 * @param visibility Visibility modifier
 * @param classTypeParameters Class-level type parameters for generic extension functions
 */
internal fun CodeFileBuilder.unitVerifyFunction(
    fakeClassName: String,
    interfaceName: String,
    methodName: String,
    visibility: FirVisibility,
    classTypeParameters: List<String>,
) {
    val capitalizedMethodName = methodName.capitalizeFirst()
    val verifierClassName = "${interfaceName}${capitalizedMethodName}CallVerifier"
    val backingFieldName = "_${methodName}Calls"
    val functionName = "verify$capitalizedMethodName"
    val typeParams = parseTypeParameters(classTypeParameters)

    // Build receiver type: FakeClassName or FakeClassName<T, K>
    val receiverType = "$fakeClassName${typeParams.usage}"

    function(functionName) {
        visibility.applyVisibility(this)
        isInline = true

        // Add type parameters without constraints (just names)
        // The constraints go in the where clause
        if (classTypeParameters.isNotEmpty()) {
            classTypeParameters.forEach { param ->
                val cleanParam = param.removePrefix("out ").removePrefix("in ").trim()
                val name = cleanParam.substringBefore(" :").trim()
                typeParam(name)
            }
        }

        // Add where clause if there are constraints
        if (typeParams.whereClause.isNotEmpty()) {
            // Remove the leading " where " prefix since we add it in the builder
            where(typeParams.whereClause.removePrefix(" where "))
        }

        receiver(receiverType)
        parameter("block", "$verifierClassName.() -> Unit")
        returns(verifierClassName)
        expressionBody = "$verifierClassName($backingFieldName.value).apply(block)"
    }
}

/**
 * Capitalizes the first character of a string.
 */
private fun String.capitalizeFirst(): String = replaceFirstChar { it.uppercase() }
