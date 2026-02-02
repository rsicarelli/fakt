// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

/**
 * Storage information for call history tracking.
 *
 * @property dataClassName Name of the data class for storing call arguments (null for Unit storage)
 * @property params Regular (non-vararg) parameters for the data class (empty for Unit storage)
 * @property isUnitStorage Whether this uses Unit as storage (0-param/vararg-only methods)
 */
data class HistoryStorageInfo(
    val dataClassName: String?,
    val params: List<Triple<String, String, Boolean>>,
    val isUnitStorage: Boolean = false,
)

/**
 * Resolves storage type info based on parameter count.
 *
 * All methods get call history:
 * - Methods with params: use data class (e.g., UserServiceGetUserCall)
 * - Methods without params (0-param, vararg-only): use Unit
 *
 * @param interfaceName Name of the interface (for collision-safe naming)
 * @param methodName Name of the method
 * @param params List of (name, type, isVararg) triples for parameters
 * @return HistoryStorageInfo for all methods
 */
fun resolveHistoryStorageType(
    interfaceName: String,
    methodName: String,
    params: List<Triple<String, String, Boolean>>,
): HistoryStorageInfo {
    val regularParams = params.filterNot { it.third } // Exclude varargs

    // 0-param or vararg-only methods use Unit as storage
    if (regularParams.isEmpty()) {
        return HistoryStorageInfo(dataClassName = null, params = emptyList(), isUnitStorage = true)
    }

    // Interface-prefixed data class for collision safety
    val dataClassName = "${interfaceName}${methodName.replaceFirstChar { it.uppercase() }}Call"
    return HistoryStorageInfo(
        dataClassName = dataClassName,
        params = regularParams,
        isUnitStorage = false,
    )
}

/**
 * Builds the history record expression for method body.
 *
 * @param interfaceName Name of the interface (for collision-safe data class naming)
 * @param methodName Name of the method
 * @param params List of (name, type, isVararg) triples for parameters
 * @return Expression string for recording call (Unit for 0-param, data class instance for params)
 */
fun buildHistoryRecordExpression(
    interfaceName: String,
    methodName: String,
    params: List<Triple<String, String, Boolean>>,
): String {
    val regularParams = params.filterNot { it.third }

    // 0-param or vararg-only methods use Unit
    if (regularParams.isEmpty()) return "Unit"

    // Interface-prefixed data class for collision safety
    val dataClassName = "${interfaceName}${methodName.replaceFirstChar { it.uppercase() }}Call"
    return "$dataClassName(${regularParams.joinToString(", ") { it.first }})"
}
