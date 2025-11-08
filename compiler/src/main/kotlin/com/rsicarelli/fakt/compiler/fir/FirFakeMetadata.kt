// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import org.jetbrains.kotlin.name.ClassId

/**
 * Metadata for a validated @Fake interface, analyzed in FIR phase and passed to IR generation.
 *
 * This data class represents the complete structural analysis of an interface that:
 * 1. Has been validated as eligible for fake generation (not sealed, not external, etc.)
 * 2. Has had its type parameters and members analyzed
 * 3. Is ready for IR phase to reconstruct GenericPattern and generate code
 *
 * Following Metro pattern: FIR phase analyzes and validates, IR phase generates.
 *
 * **Phase 3B.2 Change**: Removed `genericPattern` field - it will be reconstructed in IR phase
 * from `typeParameters` and `functions` using IrTypeParameter (which we don't have in FIR).
 *
 * **Phase 3C.3 Change**: Added `inheritedProperties` and `inheritedFunctions` to support
 * interface inheritance. These are collected from super-interfaces recursively.
 *
 * **Immutable**: Once created in FIR phase, this metadata is never modified.
 * **Thread-safe**: Stored in [FirMetadataStorage] which is thread-safe.
 *
 * @property classId Fully qualified class identifier (e.g., com.example.UserService)
 * @property simpleName Simple class name (e.g., UserService)
 * @property packageName Package name (e.g., com.example)
 * @property typeParameters Class-level type parameters with bounds (e.g., ["T", "K : Comparable<K>"])
 * @property properties Properties declared directly in this interface
 * @property functions Functions declared directly in this interface (with method-level type parameters)
 * @property inheritedProperties Properties inherited from super-interfaces (Phase 3C.3)
 * @property inheritedFunctions Functions inherited from super-interfaces (Phase 3C.3)
 * @property sourceLocation Location in source code for error reporting
 */
data class ValidatedFakeInterface(
    val classId: ClassId,
    val simpleName: String,
    val packageName: String,
    val typeParameters: List<FirTypeParameterInfo>,
    val properties: List<FirPropertyInfo>,
    val functions: List<FirFunctionInfo>,
    val inheritedProperties: List<FirPropertyInfo>,
    val inheritedFunctions: List<FirFunctionInfo>,
    val sourceLocation: FirSourceLocation,
)

/**
 * Metadata for a validated @Fake class, analyzed in FIR phase.
 *
 * Similar to [ValidatedFakeInterface] but for abstract/open classes that can be faked.
 *
 * @property classId Fully qualified class identifier
 * @property simpleName Simple class name
 * @property packageName Package name
 * @property typeParameters Class-level type parameters with bounds
 * @property abstractProperties Abstract properties requiring implementation
 * @property openProperties Open properties that can be overridden
 * @property abstractMethods Abstract methods requiring implementation
 * @property openMethods Open methods that can be overridden
 * @property sourceLocation Location in source code
 */
data class ValidatedFakeClass(
    val classId: ClassId,
    val simpleName: String,
    val packageName: String,
    val typeParameters: List<FirTypeParameterInfo>,
    val abstractProperties: List<FirPropertyInfo>,
    val openProperties: List<FirPropertyInfo>,
    val abstractMethods: List<FirFunctionInfo>,
    val openMethods: List<FirFunctionInfo>,
    val sourceLocation: FirSourceLocation,
)

/**
 * Type parameter information extracted from FIR.
 *
 * Examples:
 * - Simple: `T` → FirTypeParameterInfo("T", emptyList())
 * - Bounded: `T : Comparable<T>` → FirTypeParameterInfo("T", listOf("Comparable<T>"))
 * - Multiple bounds: `T : Comparable<T>, Serializable` → FirTypeParameterInfo("T", listOf("Comparable<T>", "Serializable"))
 *
 * @property name Type parameter name (e.g., "T", "K", "V")
 * @property bounds Upper bounds/constraints (empty for unbounded, e.g., `T : Any` is implicit)
 */
data class FirTypeParameterInfo(
    val name: String,
    val bounds: List<String>, // String representation for simplicity (e.g., "Comparable<T>")
)

/**
 * Property information extracted from FIR interface/class.
 *
 * @property name Property name
 * @property type Type as string (e.g., "String", "List<T>", "Map<K, V>")
 * @property isMutable True if `var`, false if `val`
 * @property isNullable True if type is nullable (e.g., `String?`)
 */
data class FirPropertyInfo(
    val name: String,
    val type: String, // String representation - will be resolved to IR types later
    val isMutable: Boolean,
    val isNullable: Boolean,
)

/**
 * Function information extracted from FIR interface/class.
 *
 * @property name Function name
 * @property parameters Function parameters
 * @property returnType Return type as string
 * @property isSuspend True if `suspend fun`
 * @property isInline True if `inline fun`
 * @property typeParameters Method-level type parameters (e.g., `<T>`, `<R : TValue>`)
 * @property typeParameterBounds Map of type parameter to its bound (e.g., "R" → "TValue")
 */
data class FirFunctionInfo(
    val name: String,
    val parameters: List<FirParameterInfo>,
    val returnType: String,
    val isSuspend: Boolean,
    val isInline: Boolean,
    val typeParameters: List<FirTypeParameterInfo>,
    val typeParameterBounds: Map<String, String>, // e.g., "R" → "TValue"
)

/**
 * Function parameter information.
 *
 * Phase 3C.4: Added defaultValueCode for rendering actual default value expressions.
 *
 * @property name Parameter name
 * @property type Parameter type as string
 * @property hasDefaultValue True if parameter has default value
 * @property defaultValueCode Rendered default value expression (e.g., "null", "\"GET\"", "30000L", "true").
 *                            null if no default or if rendering complex expressions failed.
 * @property isVararg True if parameter is vararg
 */
data class FirParameterInfo(
    val name: String,
    val type: String,
    val hasDefaultValue: Boolean,
    val defaultValueCode: String?,
    val isVararg: Boolean,
)

/**
 * Source location information for error reporting.
 *
 * Captured in FIR phase where source location information is most accurate.
 * Used if we need to report errors during IR generation.
 *
 * @property filePath Source file path
 * @property startLine Start line number (1-indexed)
 * @property startColumn Start column number (0-indexed)
 * @property endLine End line number (1-indexed)
 * @property endColumn End column number (0-indexed)
 */
data class FirSourceLocation(
    val filePath: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
) {
    companion object {
        /**
         * Unknown location - used as fallback when source info not available.
         */
        val UNKNOWN =
            FirSourceLocation(
                filePath = "<unknown>",
                startLine = 0,
                startColumn = 0,
                endLine = 0,
                endColumn = 0,
            )
    }

    /**
     * Human-readable location string for error messages.
     *
     * Example: "UserService.kt:42:15"
     */
    fun toDisplayString(): String = "$filePath:$startLine:$startColumn"
}
