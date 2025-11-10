// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType

/**
 * Analysis models used by both FIR and IR phases.
 *
 * These data classes represent the analyzed structure of interfaces and classes
 * that will be used for fake generation. They serve as the bridge between
 * FIR metadata and IR code generation.
 */

/**
 * Complete analysis result for an interface.
 *
 * Used by code generators to produce fake implementations.
 * Can be created from FIR metadata or legacy IR analysis.
 */
data class InterfaceAnalysis(
    val interfaceName: String,
    val typeParameters: List<String>,
    val properties: List<PropertyAnalysis>,
    val functions: List<FunctionAnalysis>,
    val sourceInterface: IrClass,
    val genericPattern: GenericPattern,
    val debugInfo: StringBuilder = StringBuilder(),
)

/**
 * Complete analysis result for a class (open or abstract).
 *
 * Distinguishes between abstract and open members for proper super delegation.
 */
data class ClassAnalysis(
    val className: String,
    val typeParameters: List<String>,
    val abstractMethods: List<FunctionAnalysis>,
    val openMethods: List<FunctionAnalysis>,
    val abstractProperties: List<PropertyAnalysis>,
    val openProperties: List<PropertyAnalysis>,
    val sourceClass: IrClass,
)

/**
 * Analysis of a property within an interface or class.
 */
data class PropertyAnalysis(
    val name: String,
    val type: IrType,
    val isMutable: Boolean,
    val isNullable: Boolean,
    val irProperty: IrProperty,
)

/**
 * Analysis of a function within an interface or class.
 */
data class FunctionAnalysis(
    val name: String,
    val parameters: List<ParameterAnalysis>,
    val returnType: IrType,
    val isSuspend: Boolean,
    val isInline: Boolean,
    val typeParameters: List<String>, // Method-level type parameters like <T>, <T, R>
    val typeParameterBounds: Map<String, String>, // Type parameter bounds like R : TValue
    val isOperator: Boolean, // Whether this function is declared with 'operator' modifier
    val extensionReceiverType: IrType?, // Extension receiver type (e.g., Vector for fun Vector.plus())
    val irFunction: IrSimpleFunction,
)

/**
 * Analysis of a function parameter.
 */
data class ParameterAnalysis(
    val name: String,
    val type: IrType,
    val hasDefaultValue: Boolean,
    val defaultValueCode: String?, // Rendered default value expression
    val isVararg: Boolean,
)
