// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Generates all call history components: data classes + verifiers + verify functions.
 *
 * For ALL methods:
 * - Methods with params: data class + full verifier + verify function
 * - 0-param/vararg-only: simplified verifier (wasCalledTimes/wasNeverCalled) + verify function
 *
 * @param fakeClassName Name of the fake implementation class
 * @param interfaceName Name of the interface (for collision-safe data class naming)
 * @param methods List of method specs to generate history for
 * @param visibility Visibility modifier for generated code
 * @param classTypeParameters Class-level type parameters that need to be erased to Any?
 * @return Generated code string with all call history components
 */
fun generateCallHistoryComponents(
    fakeClassName: String,
    interfaceName: String,
    methods: List<MethodSpec>,
    visibility: FirVisibility,
    classTypeParameters: List<String> = emptyList(),
): String {
    if (methods.isEmpty()) return ""

    val methodsWithParams = methods.filter { it.params.any { p -> !p.third } }
    val zeroParamMethods = methods.filter { it.params.none { p -> !p.third } }
    val classTypeParamNames = classTypeParameters.map(::extractTypeParamName).toSet()

    return buildString {
        // Data classes for methods with params
        if (methodsWithParams.isNotEmpty()) {
            appendLine("// region Call History Data Classes")
            appendLine()
            methodsWithParams.forEach { method ->
                val typeParams = classTypeParamNames + method.typeParameters.map(::extractTypeParamName).toSet()
                append(generateCallDataClass(interfaceName, method.name, method.params, visibility, typeParams))
                appendLine()
            }
            appendLine("// endregion")
            appendLine()
        }

        // Verifiers for all methods
        appendLine("// region Call History Verifiers")
        appendLine()
        methodsWithParams.forEach { method ->
            val typeParams = classTypeParamNames + method.typeParameters.map(::extractTypeParamName).toSet()
            append(generateVerifierClass(interfaceName, method.name, method.params, visibility, typeParams))
            appendLine()
        }
        zeroParamMethods.forEach { method ->
            append(generateUnitVerifierClass(interfaceName, method.name, visibility))
            appendLine()
        }
        appendLine("// endregion")
        appendLine()

        // Verify functions for all methods
        appendLine("// region Verify Functions")
        appendLine()
        methods.forEach { method ->
            val hasParams = method.params.any { !it.third }
            val verifyFn =
                if (hasParams) {
                    generateVerifyFunction(fakeClassName, interfaceName, method.name, visibility, classTypeParameters)
                } else {
                    generateUnitVerifyFunction(fakeClassName, interfaceName, method.name, visibility, classTypeParameters)
                }
            append(verifyFn)
            appendLine()
        }
        appendLine("// endregion")
    }
}

/**
 * Generates data class for a method's call recording.
 *
 * Example output:
 * ```kotlin
 * public data class UserServiceGetUserCall(val id: String)
 * ```
 *
 * @param interfaceName Interface name for collision-safe prefix
 * @param methodName Method name
 * @param params List of (name, type, isVararg) triples for parameters
 * @param visibility Visibility modifier
 * @param allTypeParams Set of ALL type parameter names to erase (class-level + method-level)
 * @return Generated data class code
 */
fun generateCallDataClass(
    interfaceName: String,
    methodName: String,
    params: List<Triple<String, String, Boolean>>,
    visibility: FirVisibility,
    allTypeParams: Set<String> = emptySet(),
): String {
    val regularParams = params.filterNot { it.third }
    if (regularParams.isEmpty()) return ""

    // Interface-prefixed class name for collision safety
    val className = "${interfaceName}${methodName.replaceFirstChar { it.uppercase() }}Call"
    val visibilityModifier = visibility.toModifierString()

    // Erase ALL type parameters (class-level + method-level) to Any?
    val paramsWithErasedTypes =
        regularParams.map { (name, type, isVararg) ->
            Triple(name, eraseTypeParameters(type, allTypeParams), isVararg)
        }

    return if (paramsWithErasedTypes.size == 1) {
        val (name, type, _) = paramsWithErasedTypes.first()
        "${visibilityModifier}data class $className(val $name: $type)\n"
    } else {
        val props = paramsWithErasedTypes.joinToString(",\n    ") { (name, type, _) -> "val $name: $type" }
        """
${visibilityModifier}data class $className(
    $props,
)
        """.trimIndent() + "\n"
    }
}

/**
 * Erases class-level type parameters to Any? in a type string.
 *
 * Examples:
 * - "T" → "Any?"
 * - "List<T>" → "List<Any?>"
 * - "Map<K, V>" → "Map<Any?, Any?>"
 * - "String" → "String" (no change)
 */
private fun eraseTypeParameters(
    type: String,
    classTypeParams: Set<String>,
): String {
    if (classTypeParams.isEmpty()) return type

    var result = type
    for (param in classTypeParams) {
        // Match whole type parameter occurrences
        // Handle: T, T?, List<T>, List<T?>, Map<K, V>, etc.
        val nonNullableRegex =
            typeParamNonNullableRegex.getOrPut(param) {
                Regex("\\b$param\\b(?!\\?)")
            }
        val nullableRegex =
            typeParamNullableRegex.getOrPut(param) {
                Regex("\\b$param\\?")
            }
        result =
            result
                .replace(nonNullableRegex, "Any?") // T → Any?
                .replace(nullableRegex, "Any?") // T? → Any?
    }
    return result
}

// Cache for compiled regex patterns to avoid recompilation
private val typeParamNonNullableRegex = mutableMapOf<String, Regex>()
private val typeParamNullableRegex = mutableMapOf<String, Regex>()

/**
 * Generates verifier class for a method.
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
 * @param params List of (name, type, isVararg) triples for parameters
 * @param visibility Visibility modifier
 * @param allTypeParams Set of ALL type parameter names to erase (class-level + method-level)
 * @return Generated verifier class code
 */
fun generateVerifierClass(
    interfaceName: String,
    methodName: String,
    params: List<Triple<String, String, Boolean>>,
    visibility: FirVisibility,
    allTypeParams: Set<String> = emptySet(),
): String {
    val regularParams = params.filterNot { it.third }
    if (regularParams.isEmpty()) return ""

    // Interface-prefixed class names for collision safety
    val dataClassName = "${interfaceName}${methodName.replaceFirstChar { it.uppercase() }}Call"
    val verifierClassName = "${dataClassName}Verifier"
    val visibilityModifier = visibility.toModifierString()

    // Erase ALL type parameters (class-level + method-level) to Any?
    val paramsWithErasedTypes =
        regularParams.map { (name, type, isVararg) ->
            Triple(name, eraseTypeParameters(type, allTypeParams), isVararg)
        }

    val wasCalledWithParams = paramsWithErasedTypes.joinToString(", ") { (name, type, _) -> "$name: $type" }
    val wasCalledWithCondition = paramsWithErasedTypes.joinToString(" && ") { (name, _, _) -> "it.$name == $name" }

    // For single-param methods, generate wasCalledInOrder with vararg
    // Skip for Result<T> because Kotlin doesn't allow Result as vararg type
    val wasCalledInOrderMethod =
        if (paramsWithErasedTypes.size == 1) {
            val (paramName, paramType, _) = paramsWithErasedTypes.first()
            // Skip vararg generation for Result types (Kotlin doesn't allow Result as vararg)
            if (paramType.startsWith("Result<")) {
                // Still generate neverCalledWith (doesn't use vararg)
                """
    ${visibilityModifier}fun neverCalledWith($paramName: $paramType): Boolean =
        calls.none { it.$paramName == $paramName }
"""
            } else {
                """
    ${visibilityModifier}fun wasCalledInOrder(vararg ${paramName}s: $paramType): Boolean =
        calls.map { it.$paramName } == ${paramName}s.toList()

    ${visibilityModifier}fun neverCalledWith($paramName: $paramType): Boolean =
        calls.none { it.$paramName == $paramName }
"""
            }
        } else {
            // For multi-param methods, skip wasCalledInOrder (less common use case)
            ""
        }

    return """
${visibilityModifier}class $verifierClassName(private val calls: List<$dataClassName>) {
    ${visibilityModifier}fun wasCalledTimes(n: Int): Boolean = calls.size == n

    ${visibilityModifier}fun wasCalledWith($wasCalledWithParams): Boolean =
        calls.any { $wasCalledWithCondition }

    ${visibilityModifier}fun wasNeverCalled(): Boolean = calls.isEmpty()
$wasCalledInOrderMethod
    ${visibilityModifier}val lastOrNull: $dataClassName? get() = calls.lastOrNull()

    ${visibilityModifier}val first: $dataClassName get() = calls.first()

    ${visibilityModifier}val all: List<$dataClassName> get() = calls
}
        """.trimIndent() + "\n"
}

/**
 * Generates verify function for a method.
 *
 * Example output for non-generic:
 * ```kotlin
 * public inline fun FakeUserServiceImpl.verifyGetUser(
 *     block: UserServiceGetUserCallVerifier.() -> Unit
 * ): UserServiceGetUserCallVerifier = UserServiceGetUserCallVerifier(_getUserCalls.value).apply(block)
 * ```
 *
 * Example output for generic:
 * ```kotlin
 * public inline fun <T> FakeDataCacheImpl<T>.verifyStore(
 *     block: DataCacheStoreCallVerifier.() -> Unit
 * ): DataCacheStoreCallVerifier = DataCacheStoreCallVerifier(_storeCalls.value).apply(block)
 * ```
 *
 * @param fakeClassName Name of the fake implementation class
 * @param interfaceName Interface name for collision-safe class references
 * @param methodName Method name
 * @param visibility Visibility modifier
 * @param classTypeParameters Class-level type parameters for generic extension functions
 * @return Generated verify function code
 */
fun generateVerifyFunction(
    fakeClassName: String,
    interfaceName: String,
    methodName: String,
    visibility: FirVisibility,
    classTypeParameters: List<String> = emptyList(),
): String {
    val capitalizedMethodName = methodName.replaceFirstChar { it.uppercase() }
    val dataClassName = "${interfaceName}${capitalizedMethodName}Call"
    val verifierClassName = "${dataClassName}Verifier"
    val backingFieldName = "_${methodName}Calls"
    val functionName = "verify$capitalizedMethodName"
    val visibilityModifier = visibility.toModifierString()
    val typeParams = parseTypeParameters(classTypeParameters)

    return buildVerifyFunctionKDoc(methodName, functionName, verifierClassName, withParams = true) +
        "${visibilityModifier}inline fun ${typeParams.declaration}$fakeClassName${typeParams.usage}.$functionName(\n" +
        "    block: $verifierClassName.() -> Unit,\n" +
        "): $verifierClassName${typeParams.whereClause} = " +
        "$verifierClassName($backingFieldName.value).apply(block)\n"
}

/**
 * Builds KDoc for verify functions.
 */
private fun buildVerifyFunctionKDoc(
    methodName: String,
    functionName: String,
    verifierClassName: String,
    withParams: Boolean,
): String {
    val paramLines =
        if (withParams) {
            """ * - Verify specific arguments with [$verifierClassName.wasCalledWith]
 * - Access individual calls via [$verifierClassName.all], [$verifierClassName.first]"""
        } else {
            " * - Verify no calls with [$verifierClassName.wasNeverCalled]"
        }

    val exampleAssertion = if (withParams) "assertTrue(wasCalledWith(...))" else "assertFalse(wasNeverCalled())"

    return """
/**
 * Verifies call history for [$methodName] method.
 *
 * Use this to assert how [$methodName] was called during the test:
 * - Check call count with [$verifierClassName.wasCalledTimes]
$paramLines
 *
 * Example:
 * ```kotlin
 * fake.$functionName {
 *     assertTrue(wasCalledTimes(2))
 *     $exampleAssertion
 * }
 * ```
 *
 * @param block Verification block with access to [$verifierClassName] methods
 * @return The verifier instance for additional assertions if needed
 */
        """.trimIndent()
}

private fun FirVisibility.toModifierString(): String =
    when (this) {
        FirVisibility.PUBLIC -> "public "
        FirVisibility.INTERNAL -> "internal "
        FirVisibility.PRIVATE -> "private "
        FirVisibility.PROTECTED -> "protected "
    }

/**
 * Generates simplified verifier class for 0-param/vararg-only methods.
 *
 * These verifiers only have wasCalledTimes and wasNeverCalled since there's
 * no argument data to verify.
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
 * @return Generated verifier class code
 */
fun generateUnitVerifierClass(
    interfaceName: String,
    methodName: String,
    visibility: FirVisibility,
): String {
    val verifierClassName = "${interfaceName}${methodName.replaceFirstChar { it.uppercase() }}CallVerifier"
    val visibilityModifier = visibility.toModifierString()

    return """
${visibilityModifier}class $verifierClassName(private val calls: List<Unit>) {
    ${visibilityModifier}fun wasCalledTimes(n: Int): Boolean = calls.size == n

    ${visibilityModifier}fun wasNeverCalled(): Boolean = calls.isEmpty()
}
        """.trimIndent() + "\n"
}

/**
 * Generates verify function for 0-param/vararg-only methods.
 *
 * Uses simplified verifier with only wasCalledTimes and wasNeverCalled.
 */
fun generateUnitVerifyFunction(
    fakeClassName: String,
    interfaceName: String,
    methodName: String,
    visibility: FirVisibility,
    classTypeParameters: List<String> = emptyList(),
): String {
    val capitalizedMethodName = methodName.replaceFirstChar { it.uppercase() }
    val verifierClassName = "${interfaceName}${capitalizedMethodName}CallVerifier"
    val backingFieldName = "_${methodName}Calls"
    val functionName = "verify$capitalizedMethodName"
    val visibilityModifier = visibility.toModifierString()
    val typeParams = parseTypeParameters(classTypeParameters)

    return buildVerifyFunctionKDoc(methodName, functionName, verifierClassName, withParams = false) +
        "${visibilityModifier}inline fun ${typeParams.declaration}$fakeClassName${typeParams.usage}.$functionName(\n" +
        "    block: $verifierClassName.() -> Unit,\n" +
        "): $verifierClassName${typeParams.whereClause} = " +
        "$verifierClassName($backingFieldName.value).apply(block)\n"
}
