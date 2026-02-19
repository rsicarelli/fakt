// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Generates the public backing field for method call history.
 *
 * Creates: `public val methodNameCalls = MutableStateFlow<List<MethodNameCall>>(emptyList())` Or
 * for 0-param: `public val methodNameCalls = MutableStateFlow<List<Unit>>(emptyList())`
 *
 * @param methodName Name of the method to track
 * @param dataClassName Name of the data class storing call arguments, or null for Unit storage
 * @param visibility Visibility modifier to apply
 */
fun ClassBuilder.callHistoryBackingField(
    methodName: String,
    dataClassName: String?,
    visibility: FirVisibility,
) {
    val fieldName = "${methodName}Calls"
    val storageType = dataClassName ?: "Unit"

    property(fieldName, "MutableStateFlow<List<$storageType>>") {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            FirVisibility.PRIVATE,
            FirVisibility.PROTECTED -> public()
        }
        initializer = "MutableStateFlow(emptyList())"
    }
}

/**
 * Generates the public call history field for property getter tracking.
 *
 * Creates: `public val propertyNameCalls = MutableStateFlow<List<Unit>>(emptyList())`
 *
 * @param propertyName Name of the property to track
 * @param visibility Visibility modifier to apply
 */
fun ClassBuilder.propertyGetterCallHistoryField(propertyName: String, visibility: FirVisibility) {
    val fieldName = "${propertyName}Calls"

    property(fieldName, "MutableStateFlow<List<Unit>>") {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            FirVisibility.PRIVATE,
            FirVisibility.PROTECTED -> public()
        }
        initializer = "MutableStateFlow(emptyList())"
    }
}

/**
 * Generates the public call history field for property setter tracking.
 *
 * Creates: `public val setPropertyNameCalls = MutableStateFlow<List<Unit>>(emptyList())`
 *
 * @param propertyName Name of the property to track
 * @param visibility Visibility modifier to apply
 */
fun ClassBuilder.propertySetterCallHistoryField(propertyName: String, visibility: FirVisibility) {
    val capitalizedName = propertyName.replaceFirstChar { it.uppercase() }
    val fieldName = "set${capitalizedName}Calls"

    property(fieldName, "MutableStateFlow<List<Unit>>") {
        when (visibility) {
            FirVisibility.PUBLIC -> public()
            FirVisibility.INTERNAL -> internal()
            FirVisibility.PRIVATE,
            FirVisibility.PROTECTED -> public()
        }
        initializer = "MutableStateFlow(emptyList())"
    }
}
