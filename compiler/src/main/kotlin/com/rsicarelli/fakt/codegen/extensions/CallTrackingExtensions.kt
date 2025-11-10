// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder

/**
 * Generates call tracking StateFlow properties for a method.
 *
 * Creates pattern:
 * ```kotlin
 * private val _methodNameCallCount = MutableStateFlow(0)
 * val methodNameCallCount: StateFlow<Int> get() = _methodNameCallCount
 * ```
 *
 * Used for test infrastructure to track method invocations.
 *
 * @param methodName Name of the method to track
 */
fun ClassBuilder.callTrackingProperty(methodName: String) {
    val backingFieldName = "_${methodName}CallCount"
    val publicFieldName = "${methodName}CallCount"

    // Backing MutableStateFlow
    property(backingFieldName, "MutableStateFlow<Int>") {
        private()
        initializer = "MutableStateFlow(0)"
    }

    // Public StateFlow getter
    property(publicFieldName, "StateFlow<Int>") {
        getter = backingFieldName
    }
}

/**
 * Generates call tracking StateFlow properties for a property getter.
 *
 * Creates pattern:
 * ```kotlin
 * private val _propertyNameCallCount = MutableStateFlow(0)
 * val propertyNameCallCount: StateFlow<Int> get() = _propertyNameCallCount
 * ```
 *
 * @param propertyName Name of the property to track
 */
fun ClassBuilder.propertyGetterTracking(propertyName: String) {
    val backingFieldName = "_${propertyName}CallCount"
    val publicFieldName = "${propertyName}CallCount"

    // Backing MutableStateFlow
    property(backingFieldName, "MutableStateFlow<Int>") {
        private()
        initializer = "MutableStateFlow(0)"
    }

    // Public StateFlow getter
    property(publicFieldName, "StateFlow<Int>") {
        getter = backingFieldName
    }
}

/**
 * Generates call tracking StateFlow properties for a property setter.
 *
 * Creates pattern:
 * ```kotlin
 * private val _setPropertyNameCallCount = MutableStateFlow(0)
 * val setPropertyNameCallCount: StateFlow<Int> get() = _setPropertyNameCallCount
 * ```
 *
 * @param propertyName Name of the property to track
 */
fun ClassBuilder.propertySetterTracking(propertyName: String) {
    val capitalizedName = propertyName.replaceFirstChar { it.uppercase() }
    val backingFieldName = "_set${capitalizedName}CallCount"
    val publicFieldName = "set${capitalizedName}CallCount"

    // Backing MutableStateFlow
    property(backingFieldName, "MutableStateFlow<Int>") {
        private()
        initializer = "MutableStateFlow(0)"
    }

    // Public StateFlow getter
    property(publicFieldName, "StateFlow<Int>") {
        getter = backingFieldName
    }
}
