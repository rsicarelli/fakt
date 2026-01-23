// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Generates call tracking StateFlow properties for a method.
 *
 * Creates pattern:
 * ```kotlin
 * private val _methodNameCallCount = MutableStateFlow(0)
 * public val methodNameCallCount: StateFlow<Int> get() = _methodNameCallCount
 * ```
 *
 * Used for test infrastructure to track method invocations.
 *
 * @param methodName Name of the method to track
 * @param visibility Visibility modifier to apply to the public getter
 */
fun ClassBuilder.callTrackingProperty(methodName: String, visibility: FirVisibility) {
    val backingFieldName = "_${methodName}CallCount"
    val publicFieldName = "${methodName}CallCount"

    // Backing MutableStateFlow
    property(backingFieldName, "MutableStateFlow<Int>") {
        private()
        initializer = "MutableStateFlow(0)"
    }

    // Public StateFlow getter with visibility for explicitApi() support
    property(publicFieldName, "StateFlow<Int>") {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            FirVisibility.PRIVATE, FirVisibility.PROTECTED -> public()
        }
        getter = backingFieldName
    }
}

/**
 * Generates call tracking StateFlow properties for a property getter.
 *
 * Creates pattern:
 * ```kotlin
 * private val _propertyNameCallCount = MutableStateFlow(0)
 * public val propertyNameCallCount: StateFlow<Int> get() = _propertyNameCallCount
 * ```
 *
 * @param propertyName Name of the property to track
 * @param visibility Visibility modifier to apply to the public getter
 */
fun ClassBuilder.propertyGetterTracking(propertyName: String, visibility: FirVisibility) {
    val backingFieldName = "_${propertyName}CallCount"
    val publicFieldName = "${propertyName}CallCount"

    // Backing MutableStateFlow
    property(backingFieldName, "MutableStateFlow<Int>") {
        private()
        initializer = "MutableStateFlow(0)"
    }

    // Public StateFlow getter with visibility for explicitApi() support
    property(publicFieldName, "StateFlow<Int>") {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            FirVisibility.PRIVATE, FirVisibility.PROTECTED -> public()
        }
        getter = backingFieldName
    }
}

/**
 * Generates call tracking StateFlow properties for a property setter.
 *
 * Creates pattern:
 * ```kotlin
 * private val _setPropertyNameCallCount = MutableStateFlow(0)
 * public val setPropertyNameCallCount: StateFlow<Int> get() = _setPropertyNameCallCount
 * ```
 *
 * @param propertyName Name of the property to track
 * @param visibility Visibility modifier to apply to the public getter
 */
fun ClassBuilder.propertySetterTracking(propertyName: String, visibility: FirVisibility) {
    val capitalizedName = propertyName.replaceFirstChar { it.uppercase() }
    val backingFieldName = "_set${capitalizedName}CallCount"
    val publicFieldName = "set${capitalizedName}CallCount"

    // Backing MutableStateFlow
    property(backingFieldName, "MutableStateFlow<Int>") {
        private()
        initializer = "MutableStateFlow(0)"
    }

    // Public StateFlow getter with visibility for explicitApi() support
    property(publicFieldName, "StateFlow<Int>") {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            FirVisibility.PRIVATE, FirVisibility.PROTECTED -> public()
        }
        getter = backingFieldName
    }
}
