// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Generates the public getter for method call tracking.
 *
 * For methods, count is derived from call history: `val methodNameCallCount: Int get() =
 * _methodNameCalls.value.size` This eliminates redundant state updates since count can be derived
 * from history.
 *
 * @param methodName Name of the method to track
 * @param visibility Visibility modifier to apply to the public getter
 */
fun ClassBuilder.callTrackingPublicGetter(methodName: String, visibility: FirVisibility) {
    val callsFieldName = "_${methodName}Calls"
    val publicFieldName = "${methodName}CallCount"

    property(publicFieldName, "Int") {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            FirVisibility.PRIVATE,
            FirVisibility.PROTECTED -> public()
        }
        getter = "$callsFieldName.value.size"
    }
}

/**
 * Generates only the private backing field for property getter tracking.
 *
 * Creates: `private val _propertyNameCallCount = MutableStateFlow(0)`
 *
 * Properties keep a backing StateFlow because they have no call history to derive count from.
 *
 * @param propertyName Name of the property to track
 */
fun ClassBuilder.propertyGetterTrackingBackingField(propertyName: String) {
    val backingFieldName = "_${propertyName}CallCount"

    property(backingFieldName, "MutableStateFlow<Int>") {
        private()
        initializer = "MutableStateFlow(0)"
    }
}

/**
 * Generates only the public getter for property getter tracking.
 *
 * Creates: `public val propertyNameCallCount: Int get() = _propertyNameCallCount.value`
 *
 * Exposes Int directly for unified API (same as method call counts).
 *
 * @param propertyName Name of the property to track
 * @param visibility Visibility modifier to apply to the public getter
 */
fun ClassBuilder.propertyGetterTrackingPublicGetter(
    propertyName: String,
    visibility: FirVisibility,
) {
    val backingFieldName = "_${propertyName}CallCount"
    val publicFieldName = "${propertyName}CallCount"

    property(publicFieldName, "Int") {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            FirVisibility.PRIVATE,
            FirVisibility.PROTECTED -> public()
        }
        getter = "$backingFieldName.value"
    }
}

/**
 * Generates only the private backing field for property setter tracking.
 *
 * Creates: `private val _setPropertyNameCallCount = MutableStateFlow(0)`
 *
 * Properties keep a backing StateFlow because they have no call history to derive count from.
 *
 * @param propertyName Name of the property to track
 */
fun ClassBuilder.propertySetterTrackingBackingField(propertyName: String) {
    val capitalizedName = propertyName.replaceFirstChar { it.uppercase() }
    val backingFieldName = "_set${capitalizedName}CallCount"

    property(backingFieldName, "MutableStateFlow<Int>") {
        private()
        initializer = "MutableStateFlow(0)"
    }
}

/**
 * Generates only the public getter for property setter tracking.
 *
 * Creates: `public val setPropertyNameCallCount: Int get() = _setPropertyNameCallCount.value`
 *
 * Exposes Int directly for unified API (same as method call counts).
 *
 * @param propertyName Name of the property to track
 * @param visibility Visibility modifier to apply to the public getter
 */
fun ClassBuilder.propertySetterTrackingPublicGetter(
    propertyName: String,
    visibility: FirVisibility,
) {
    val capitalizedName = propertyName.replaceFirstChar { it.uppercase() }
    val backingFieldName = "_set${capitalizedName}CallCount"
    val publicFieldName = "set${capitalizedName}CallCount"

    property(publicFieldName, "Int") {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            FirVisibility.PRIVATE,
            FirVisibility.PROTECTED -> public()
        }
        getter = "$backingFieldName.value"
    }
}

/**
 * Generates the internal backing field for call history.
 *
 * Creates: `@PublishedApi internal val _methodNameCalls =
 * MutableStateFlow<List<MethodNameCall>>(emptyList())` Or for 0-param: `@PublishedApi internal val
 * _methodNameCalls = MutableStateFlow<List<Unit>>(emptyList())`
 *
 * @param methodName Name of the method to track
 * @param dataClassName Name of the data class storing call arguments, or null for Unit storage
 */
fun ClassBuilder.callHistoryBackingField(methodName: String, dataClassName: String?) {
    val backingFieldName = "_${methodName}Calls"
    val storageType = dataClassName ?: "Unit"

    property(backingFieldName, "MutableStateFlow<List<$storageType>>") {
        // @PublishedApi allows public inline functions to access internal members
        this.annotation("PublishedApi")
        internal()
        initializer = "MutableStateFlow(emptyList())"
    }
}
