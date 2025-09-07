// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.analysis

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty

/**
 * Pure interface analysis logic for discovering and analyzing @Fake annotated interfaces.
 *
 * This module is responsible for:
 * - Discovering @Fake annotated interfaces
 * - Analyzing interface structure (methods, properties, generics)
 * - Extracting method signatures dynamically
 * - Thread-safety validation
 * - Dependency analysis for cross-module coordination
 *
 * Key principle: Pure analysis with no code generation logic.
 */
interface InterfaceAnalyzer {

    /**
     * Analyze a @Fake annotated interface and extract structural information.
     *
     * @param sourceInterface The interface to analyze
     * @return Analyzed interface structure
     */
    fun analyzeInterface(sourceInterface: IrClass): InterfaceAnalysis

    /**
     * Discover all @Fake annotated interfaces in a module.
     *
     * @param moduleClasses All classes in the module
     * @return List of @Fake annotated interfaces
     */
    fun discoverFakeInterfaces(moduleClasses: List<IrClass>): List<IrClass>

    /**
     * Validate that an interface is suitable for fake generation.
     *
     * @param sourceInterface Interface to validate
     * @return Validation result with any errors
     */
    fun validateInterface(sourceInterface: IrClass): ValidationResult
}

/**
 * Results of interface analysis containing all information needed for code generation.
 */
data class InterfaceAnalysis(
    val sourceInterface: Any?, // Will be IrClass in real implementation
    val interfaceName: String,
    val packageName: String,
    val methods: List<MethodAnalysis>,
    val properties: List<PropertyAnalysis>,
    val generics: List<GenericAnalysis>,
    val annotations: AnnotationAnalysis,
    val dependencies: List<DependencyAnalysis>
)

/**
 * Analysis of a single method in the interface.
 */
data class MethodAnalysis(
    val function: Any?, // Will be IrSimpleFunction in real implementation
    val name: String,
    val parameters: List<ParameterAnalysis>,
    val returnType: TypeAnalysis,
    val isSuspend: Boolean,
    val modifiers: Set<MethodModifier>
)

/**
 * Analysis of a single property in the interface.
 */
data class PropertyAnalysis(
    val property: Any?, // Will be IrProperty in real implementation
    val name: String,
    val type: TypeAnalysis,
    val hasGetter: Boolean,
    val hasSetter: Boolean,
    val modifiers: Set<PropertyModifier>
)

/**
 * Analysis of method parameters.
 */
data class ParameterAnalysis(
    val name: String,
    val type: TypeAnalysis,
    val hasDefaultValue: Boolean,
    val isVararg: Boolean
)

/**
 * Type information analysis.
 */
data class TypeAnalysis(
    val qualifiedName: String,
    val isNullable: Boolean,
    val genericArguments: List<TypeAnalysis>,
    val isBuiltin: Boolean
)

/**
 * Generic type parameter analysis.
 */
data class GenericAnalysis(
    val name: String,
    val bounds: List<TypeAnalysis>,
    val variance: GenericVariance
)

/**
 * @Fake annotation configuration analysis.
 */
data class AnnotationAnalysis(
    val trackCalls: Boolean,
    val builder: Boolean,
    val concurrent: Boolean,
    val scope: String,
    val dependencies: List<String>
)

/**
 * Dependency analysis for cross-module coordination.
 */
data class DependencyAnalysis(
    val typeName: String,
    val isAvailable: Boolean,
    val module: String?
)

/**
 * Validation results.
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}

enum class MethodModifier {
    OVERRIDE, ABSTRACT, OPEN, FINAL, INLINE, SUSPEND
}

enum class PropertyModifier {
    OVERRIDE, ABSTRACT, OPEN, FINAL, CONST, LATEINIT
}

enum class GenericVariance {
    INVARIANT, COVARIANT, CONTRAVARIANT
}
