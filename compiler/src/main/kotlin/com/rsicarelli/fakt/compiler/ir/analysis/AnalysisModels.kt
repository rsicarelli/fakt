// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/*
 * Analysis models used by both FIR and IR phases.
 *
 * These data classes represent the analyzed structure of interfaces and classes
 * that will be used for fake generation. They serve as the bridge between
 * FIR metadata and IR code generation.
 */

/**
 * Complete analysis result for an interface.
 *
 * Used by code generators to produce fake implementations. Can be created from FIR metadata or
 * legacy IR analysis.
 *
 * @property generateCallHistory Controls whether call history and call count tracking code is
 *   generated. Resolved from @Fake annotation's callHistory attribute and plugin-level
 *   enableCallHistory default.
 * @property generateMutableBehaviors Controls whether mutable behavior support is generated.
 *   Resolved from @Fake annotation's mutability attribute and plugin-level enableMutableFakes
 *   default. When true, behavior properties are `internal var` and a `configure {}` method is
 *   generated.
 * @property packageName Package of the source interface (e.g. "com.example"). Pre-stored in 3.1.d.2
 *   so that [com.rsicarelli.fakt.compiler.ir.generation.CodeGenerator] no longer needs to call
 *   `IrClass.packageFqName`.
 */
data class InterfaceAnalysis(
    val interfaceName: String,
    val qualifiedSourceName: String = interfaceName,
    val packageName: String = "",
    val typeParameters: List<String>,
    val properties: List<PropertyAnalysis>,
    val functions: List<FunctionAnalysis>,
    val genericPattern: GenericPattern,
    val debugInfo: StringBuilder = StringBuilder(),
    val visibility: FirVisibility = FirVisibility.PUBLIC,
    val annotations: List<AnnotationAnalysis> = emptyList(),
    val generateCallHistory: Boolean = true,
    val generateMutableBehaviors: Boolean = false,
)

/**
 * Complete analysis result for a class (open or abstract).
 *
 * Distinguishes between abstract and open members for proper super delegation.
 *
 * @property generateCallHistory Controls whether call history and call count tracking code is
 *   generated. Resolved from @Fake annotation's callHistory attribute and plugin-level
 *   enableCallHistory default.
 * @property generateMutableBehaviors Controls whether mutable behavior support is generated.
 *   Resolved from @Fake annotation's mutability attribute and plugin-level enableMutableFakes
 *   default. When true, behavior properties are `internal var` and a `configure {}` method is
 *   generated.
 * @property packageName Package of the source class (e.g. "com.example"). Pre-stored in 3.1.d.2 so
 *   that [com.rsicarelli.fakt.compiler.ir.generation.CodeGenerator] no longer needs to call
 *   `IrClass.packageFqName`.
 */
data class ClassAnalysis(
    val className: String,
    val qualifiedSourceName: String = className,
    val packageName: String = "",
    val typeParameters: List<String>,
    val abstractMethods: List<FunctionAnalysis>,
    val openMethods: List<FunctionAnalysis>,
    val abstractProperties: List<PropertyAnalysis>,
    val openProperties: List<PropertyAnalysis>,
    val constructorParameters: List<ConstructorParameterInfo> = emptyList(),
    val visibility: FirVisibility = FirVisibility.PUBLIC,
    val annotations: List<AnnotationAnalysis> = emptyList(),
    val generateCallHistory: Boolean = true,
    val generateMutableBehaviors: Boolean = false,
)

/**
 * Constructor parameter from the parent abstract class.
 *
 * @property name Parameter name
 * @property typeString Rendered type string (e.g., "String", "Int")
 * @property hasDefault Whether the parameter has a default value
 */
data class ConstructorParameterInfo(
    val name: String,
    val typeString: String,
    val hasDefault: Boolean,
)

/**
 * Analysis of a property within an interface or class.
 *
 * Note: `irProperty: IrProperty` was removed in 3.1.d.2 — it had zero readers outside the
 * FirToIrTransformer, which now stores the pre-rendered [RenderedType] instead.
 */
data class PropertyAnalysis(
    val name: String,
    val type: org.jetbrains.kotlin.ir.types.IrType,
    val isMutable: Boolean,
    val isNullable: Boolean,
)

/**
 * Analysis of a function within an interface or class.
 *
 * Note: `irFunction: IrSimpleFunction` was removed in 3.1.d.2 — it had zero readers outside the
 * FirToIrTransformer, which now stores the pre-rendered [RenderedType] instead.
 */
data class FunctionAnalysis(
    val name: String,
    val parameters: List<ParameterAnalysis>,
    val returnType: org.jetbrains.kotlin.ir.types.IrType,
    val isSuspend: Boolean,
    val isInline: Boolean,
    val typeParameters: List<String>, // Method-level type parameters like <T>, <T, R>
    val typeParameterBounds: Map<String, String>, // Type parameter bounds like R : TValue
    val isOperator: Boolean, // Whether this function is declared with 'operator' modifier
    val extensionReceiverType:
        org.jetbrains.kotlin.ir.types.IrType?, // Extension receiver type (e.g., Vector for fun
    // Vector.plus())
)

/** Analysis of a function parameter. */
data class ParameterAnalysis(
    val name: String,
    val type: org.jetbrains.kotlin.ir.types.IrType,
    val hasDefaultValue: Boolean,
    val defaultValueCode: String?, // Rendered default value expression
    val isVararg: Boolean,
)

/**
 * Analysis of an annotation on a declaration.
 *
 * @property simpleName Simple annotation name (e.g., "OptIn", "Deprecated")
 * @property fullyQualifiedName Fully qualified name for imports (e.g., "kotlin.OptIn")
 * @property renderedArguments Pre-rendered argument strings (e.g., ["ExperimentalApi::class"])
 * @property isOptInMarker True if this annotation is marked with @RequiresOptIn. When true, the
 *   generated fake needs @OptIn(ThisAnnotation::class) to compile.
 */
data class AnnotationAnalysis(
    val simpleName: String,
    val fullyQualifiedName: String,
    val renderedArguments: List<String> = emptyList(),
    val isOptInMarker: Boolean = false,
)
