// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.transform

import com.rsicarelli.fakt.compiler.ir.analysis.ClassAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.FunctionAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.GenericPattern
import com.rsicarelli.fakt.compiler.ir.analysis.GenericPatternAnalyzer
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.ParameterAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.PropertyAnalysis
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType

/**
 * Metadata for IR code generation, transformed from FIR ValidatedFakeInterface.
 *
 * This is the **bridge** between FIR phase (string-based types) and IR phase (IrTypes).
 * Eliminates the need for re-analyzing IrClass instances.
 *
 * This API enables proper FIR→IR communication following Metro pattern:
 * - FIR analyzes and extracts metadata (strings)
 * - FirToIrTransformer resolves strings → IrTypes and lookups IR nodes
 * - IR generation uses this metadata WITHOUT re-analysis
 *
 * **Performance Optimization**: `genericPattern` is computed lazily on first access.
 * This avoids expensive pattern analysis during FIR→IR transformation for interfaces
 * that are skipped by caching or fail validation. Analysis only happens when code
 * generation actually needs the pattern information (~40% cache hit rate).
 *
 * @property interfaceName Simple interface name (e.g., "UserRepository")
 * @property packageName Package name (e.g., "com.example")
 * @property typeParameters Class-level type parameters with bounds
 *     (e.g., ["T", "K : Comparable<K>"])
 * @property properties All interface properties with resolved IrTypes
 * @property functions All interface functions with resolved IrTypes
 * @property genericPattern Classification of generic usage
 *     (NoGenerics, ClassLevel, MethodLevel, Mixed) - computed lazily
 * @property sourceInterface Original IrClass for code generation context
 */
class IrGenerationMetadata internal constructor(
    val interfaceName: String,
    val packageName: String,
    val typeParameters: List<String>,
    val properties: List<IrPropertyMetadata>,
    val functions: List<IrFunctionMetadata>,
    val sourceInterface: IrClass,
    private val patternAnalyzer: GenericPatternAnalyzer,
) {
    /**
     * Lazy generic pattern analysis - computed on first access only.
     *
     * Most interfaces have no generics or simple patterns. Deferring this
     * expensive analysis (25-40% of FIR→IR transform time) until actually
     * needed provides significant performance improvement.
     *
     * Thread-safe via Kotlin's lazy delegate (SYNCHRONIZED mode by default).
     */
    val genericPattern: GenericPattern by lazy {
        patternAnalyzer.analyzeInterface(sourceInterface)
    }
}

/**
 * Property metadata with resolved IR types.
 *
 * Transformed from FirPropertyInfo (strings) to IR-ready structure.
 *
 * @property name Property name
 * @property type Resolved IrType (from FIR string representation)
 * @property isMutable true for `var`, false for `val`
 * @property isNullable true if type is nullable (T?)
 * @property irProperty Original IR property node (for code generation)
 */
data class IrPropertyMetadata(
    val name: String,
    val type: IrType,
    val isMutable: Boolean,
    val isNullable: Boolean,
    val irProperty: IrProperty,
)

/**
 * Function metadata with resolved IR types.
 *
 * Transformed from FirFunctionInfo (strings) to IR-ready structure.
 *
 * @property name Function name
 * @property parameters Function parameters with resolved IrTypes
 * @property returnType Resolved return IrType (from FIR string representation)
 * @property isSuspend true if function is suspend
 * @property isInline true if function is inline
 * @property typeParameters Method-level type parameters with bounds (e.g., ["T", "R : Comparable<R>"])
 * @property typeParameterBounds Method-level type parameter bounds map (e.g., "R" → "TValue")
 * @property irFunction Original IR function node (for code generation)
 */
data class IrFunctionMetadata(
    val name: String,
    val parameters: List<IrParameterMetadata>,
    val returnType: IrType,
    val isSuspend: Boolean,
    val isInline: Boolean,
    val typeParameters: List<String>,
    val typeParameterBounds: Map<String, String>,
    val isOperator: Boolean,
    val extensionReceiverType: IrType?,
    val irFunction: IrSimpleFunction,
)

/**
 * Parameter metadata with resolved IR types.
 *
 * Transformed from FirParameterInfo (strings) to IR-ready structure.
 *
 * Added defaultValueCode for default parameter support in generated code.
 *
 * @property name Parameter name
 * @property type Resolved IrType (from FIR string representation)
 * @property hasDefaultValue true if parameter has default value
 * @property defaultValueCode Rendered default value code (e.g., "null", "\"GET\"", "30000L")
 * @property isVararg true if parameter is vararg
 */
data class IrParameterMetadata(
    val name: String,
    val type: IrType,
    val hasDefaultValue: Boolean,
    val defaultValueCode: String?,
    val isVararg: Boolean,
)

/**
 * Metadata for IR code generation from abstract classes, transformed from FIR ValidatedFakeClass.
 *
 * This is the **bridge** between FIR phase (string-based types) and IR phase (IrTypes) for abstract classes.
 * Similar to IrGenerationMetadata but separates abstract and open members.
 *
 * Added to support abstract class fake generation.
 * - FIR analyzes and extracts metadata (strings) with abstract/open separation
 * - FirToIrTransformer resolves strings → IrTypes and lookups IR nodes
 * - IR generation uses this metadata WITHOUT re-analysis
 *
 * **Performance Optimization**: `genericPattern` is computed lazily on first access.
 * See IrGenerationMetadata for detailed rationale.
 *
 * @property className Simple class name (e.g., "AbstractRepository")
 * @property packageName Package name (e.g., "com.example")
 * @property typeParameters Class-level type parameters with bounds
 *     (e.g., ["T", "K : Comparable<K>"])
 * @property abstractProperties Abstract properties (must be implemented)
 * @property openProperties Open properties (can be overridden)
 * @property abstractMethods Abstract methods (must be implemented)
 * @property openMethods Open methods (can be overridden)
 * @property genericPattern Classification of generic usage
 *     (NoGenerics, ClassLevel, MethodLevel, Mixed) - computed lazily
 * @property sourceClass Original IrClass for code generation context
 */
class IrClassGenerationMetadata internal constructor(
    val className: String,
    val packageName: String,
    val typeParameters: List<String>,
    val abstractProperties: List<IrPropertyMetadata>,
    val openProperties: List<IrPropertyMetadata>,
    val abstractMethods: List<IrFunctionMetadata>,
    val openMethods: List<IrFunctionMetadata>,
    val sourceClass: IrClass,
    private val patternAnalyzer: GenericPatternAnalyzer,
) {
    /**
     * Lazy generic pattern analysis - computed on first access only.
     * See IrGenerationMetadata.genericPattern for details.
     */
    val genericPattern: GenericPattern by lazy {
        patternAnalyzer.analyzeInterface(sourceClass)
    }
}

/**
 * Adapter function: Convert IrGenerationMetadata to InterfaceAnalysis.
 *
 * This allows reusing existing code generators (ImplementationGenerator,
 * FactoryGenerator, ConfigurationDslGenerator) without refactoring them
 * to accept IrGenerationMetadata directly.
 *
 * Adapter pattern for backward compatibility.
 * **Future**: Refactor generators to accept IrGenerationMetadata directly.
 *
 * @return InterfaceAnalysis compatible with existing generators
 */
fun IrGenerationMetadata.toInterfaceAnalysis(): InterfaceAnalysis =
    InterfaceAnalysis(
        interfaceName = interfaceName,
        typeParameters = typeParameters,
        properties = properties.map { it.toPropertyAnalysis() },
        functions = functions.map { it.toFunctionAnalysis() },
        sourceInterface = sourceInterface,
        genericPattern = genericPattern,
        debugInfo = StringBuilder("Generated from FIR metadata (FIR metadata)"),
    )

/**
 * Adapter function: Convert IrClassGenerationMetadata to ClassAnalysis.
 *
 * Use ClassAnalysis to preserve abstract/open distinction
 * for proper super delegation generation.
 *
 * Unlike toInterfaceAnalysis(), this keeps abstract and open members separate
 * so generators can apply the correct defaults:
 * - Abstract members → error() defaults
 * - Open members → super.method() defaults
 *
 * @return ClassAnalysis with separated abstract and open members
 */
fun IrClassGenerationMetadata.toClassAnalysis(): ClassAnalysis =
    ClassAnalysis(
        className = className,
        typeParameters = typeParameters,
        abstractMethods = abstractMethods.map { it.toFunctionAnalysis() },
        openMethods = openMethods.map { it.toFunctionAnalysis() },
        abstractProperties = abstractProperties.map { it.toPropertyAnalysis() },
        openProperties = openProperties.map { it.toPropertyAnalysis() },
        sourceClass = sourceClass,
    )

/**
 * Convert IrPropertyMetadata to PropertyAnalysis.
 */
private fun IrPropertyMetadata.toPropertyAnalysis(): PropertyAnalysis =
    PropertyAnalysis(
        name = name,
        type = type,
        isMutable = isMutable,
        isNullable = isNullable,
        irProperty = irProperty,
    )

/**
 * Convert IrFunctionMetadata to FunctionAnalysis.
 */
private fun IrFunctionMetadata.toFunctionAnalysis(): FunctionAnalysis =
    FunctionAnalysis(
        name = name,
        parameters = parameters.map { it.toParameterAnalysis() },
        returnType = returnType,
        isSuspend = isSuspend,
        isInline = isInline,
        typeParameters = typeParameters,
        typeParameterBounds = typeParameterBounds,
        isOperator = isOperator,
        extensionReceiverType = extensionReceiverType,
        irFunction = irFunction,
    )

/**
 * Convert IrParameterMetadata to ParameterAnalysis.
 *
 * Pass through defaultValueCode for default parameter support.
 */
private fun IrParameterMetadata.toParameterAnalysis(): ParameterAnalysis =
    ParameterAnalysis(
        name = name,
        type = type,
        hasDefaultValue = hasDefaultValue,
        defaultValueCode = defaultValueCode,
        isVararg = isVararg,
    )
