// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.codegen.builder.PropertyBuilder

/**
 * Creates a StateFlow property with backing MutableStateFlow.
 *
 * Generates pattern:
 * ```kotlin
 * private val {name}Value: StateFlow<T> = MutableStateFlow(defaultValue)
 * override val {name}: StateFlow<T>
 *     get() = {name}Value
 * ```
 *
 * @param name Property name
 * @param elementType Type of elements in StateFlow (e.g., "User", "List<String>")
 * @param defaultValue Initial value expression
 */
fun ClassBuilder.stateFlowProperty(
    name: String,
    elementType: String,
    defaultValue: String,
) {
    // Backing property
    property("${name}Value", "StateFlow<$elementType>") {
        private()
        initializer = "MutableStateFlow($defaultValue)"
    }

    // Public override property
    property(name, "StateFlow<$elementType>") {
        override()
        getter = "${name}Value"
    }
}

/**
 * Creates a behavior property for a function.
 *
 * Generates pattern:
 * ```kotlin
 * private var {methodName}Behavior: (Params) -> ReturnType = { defaultValue }
 * ```
 *
 * @param methodName Name of the method this behavior is for
 * @param paramTypes List of parameter types (e.g., ["String", "Int"])
 * @param returnType Return type of the function
 * @param defaultValue Default behavior lambda
 */
fun ClassBuilder.behaviorProperty(
    methodName: String,
    paramTypes: List<String>,
    returnType: String,
    defaultValue: String,
) {
    val functionType =
        if (paramTypes.isEmpty()) {
            "() -> $returnType"
        } else {
            "(${paramTypes.joinToString(", ")}) -> $returnType"
        }

    property("${methodName}Behavior", functionType) {
        private()
        mutable()
        initializer = defaultValue
    }
}

/**
 * Creates a suspend behavior property for a suspend function.
 *
 * Generates pattern:
 * ```kotlin
 * private var {methodName}Behavior: suspend (Params) -> ReturnType = { defaultValue }
 * ```
 *
 * @param methodName Name of the suspend method
 * @param paramTypes List of parameter types
 * @param returnType Return type of the function
 * @param defaultValue Default behavior lambda
 */
fun ClassBuilder.suspendBehaviorProperty(
    methodName: String,
    paramTypes: List<String>,
    returnType: String,
    defaultValue: String,
) {
    val functionType =
        if (paramTypes.isEmpty()) {
            "suspend () -> $returnType"
        } else {
            "suspend (${paramTypes.joinToString(", ")}) -> $returnType"
        }

    property("${methodName}Behavior", functionType) {
        private()
        mutable()
        initializer = defaultValue
    }
}

/**
 * Creates a nullable behavior property for an open class method.
 *
 * Generates pattern:
 * ```kotlin
 * private var {methodName}Behavior: ((Params) -> ReturnType)? = null
 * ```
 *
 * Used for open methods in classes that should delegate to super when unconfigured.
 *
 * @param methodName Name of the method this behavior is for
 * @param paramTypes List of parameter types
 * @param returnType Return type of the function
 */
fun ClassBuilder.nullableBehaviorProperty(
    methodName: String,
    paramTypes: List<String>,
    returnType: String,
) {
    val functionType =
        if (paramTypes.isEmpty()) {
            "(() -> $returnType)?"
        } else {
            "((${paramTypes.joinToString(", ")}) -> $returnType)?"
        }

    property("${methodName}Behavior", functionType) {
        private()
        mutable()
        initializer = "null"
    }
}

/**
 * Creates a nullable suspend behavior property for an open class suspend method.
 *
 * Generates pattern:
 * ```kotlin
 * private var {methodName}Behavior: (suspend (Params) -> ReturnType)? = null
 * ```
 *
 * Used for open suspend methods in classes that should delegate to super when unconfigured.
 *
 * @param methodName Name of the suspend method
 * @param paramTypes List of parameter types
 * @param returnType Return type of the function
 */
fun ClassBuilder.nullableSuspendBehaviorProperty(
    methodName: String,
    paramTypes: List<String>,
    returnType: String,
) {
    val functionType =
        if (paramTypes.isEmpty()) {
            "(suspend () -> $returnType)?"
        } else {
            "(suspend (${paramTypes.joinToString(", ")}) -> $returnType)?"
        }

    property("${methodName}Behavior", functionType) {
        private()
        mutable()
        initializer = "null"
    }
}

/**
 * Creates a simple mutable property with default value.
 *
 * Generates pattern:
 * ```kotlin
 * private var {name}: Type = defaultValue
 * ```
 */
fun ClassBuilder.mutableProperty(
    name: String,
    type: String,
    defaultValue: String,
) {
    property(name, type) {
        private()
        mutable()
        initializer = defaultValue
    }
}

/**
 * Configures this property as a behavior property.
 *
 * Extension for PropertyBuilder to apply common behavior property settings.
 */
fun PropertyBuilder.asBehavior() {
    private()
    mutable()
}

/**
 * Configures this property as a StateFlow backing property.
 */
fun PropertyBuilder.asStateFlowBacking() {
    private()
}

/**
 * Configures this property as an override with getter.
 */
fun PropertyBuilder.asOverrideWithGetter(getterExpression: String) {
    override()
    getter = getterExpression
}
