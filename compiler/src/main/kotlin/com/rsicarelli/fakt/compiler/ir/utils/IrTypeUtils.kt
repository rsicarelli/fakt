// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.utils

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

/**
 * Determines if a function is a special compiler-generated function that should be skipped.
 *
 * Special functions include:
 * - Data class generated functions (equals, hashCode, toString, componentN, copy)
 * - Functions that don't need fake implementations
 *
 * @return true if this is a special function to skip, false otherwise
 */
internal fun IrSimpleFunction.isSpecialFunction(): Boolean {
    val name = name.asString()
    return name in setOf("equals", "hashCode", "toString") ||
        name.startsWith("component") ||
        name == "copy"
}
