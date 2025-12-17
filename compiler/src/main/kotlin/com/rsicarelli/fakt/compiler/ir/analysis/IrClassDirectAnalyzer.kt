// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import com.rsicarelli.fakt.compiler.core.telemetry.FaktLogger
import com.rsicarelli.fakt.compiler.core.types.TypeResolution
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.types.isNullable

/**
 * Analyzes IrClass directly to extract interface metadata.
 *
 * Works for both source and compiled classes during test compilation.
 * Produces the same InterfaceAnalysis format as FIR-based analysis, enabling
 * reuse of existing generators without modification.
 *
 * This analyzer is used during test compilation to analyze compiled main classes
 * that are loaded via IrPluginContext.referenceClass().
 *
 * @property typeResolver Converts IrType to Kotlin string representations
 * @property logger Logger for debug output
 */
internal class IrClassDirectAnalyzer(
    private val typeResolver: TypeResolution,
    private val logger: FaktLogger,
) {
    /**
     * Analyzes an interface IrClass to produce complete metadata for code generation.
     *
     * Extracts:
     * - Type parameters with bounds
     * - Declared properties
     * - Declared functions (filtering synthetic members)
     * - Inherited members (recursively from super-interfaces)
     * - Generic pattern classification
     *
     * @param irClass The interface IrClass to analyze
     * @return Complete InterfaceAnalysis ready for code generation
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    fun analyzeInterface(irClass: IrClass): InterfaceAnalysis {
        logger.debug("Analyzing interface: ${irClass.name.asString()}")

        // 1. Basic info
        val interfaceName = irClass.name.asString()

        // 2. Type parameters with bounds
        val typeParameters = irClass.typeParameters.map { formatTypeParameter(it) }

        // 3. Declared properties
        val declaredProps =
            irClass.declarations
                .filterIsInstance<IrProperty>()
                .map { analyzeProperty(it) }

        // 4. Declared functions (filter synthetic)
        val declaredFuncs =
            irClass.declarations
                .filterIsInstance<IrSimpleFunction>()
                .filterNot { it.isSynthetic() }
                .map { analyzeFunction(it) }

        // 5. Inherited members
        val (inheritedProps, inheritedFuncs) = extractInheritedMembers(irClass)

        // 6. Generic pattern analysis
        val genericPattern = GenericPatternAnalyzer().analyzeInterface(irClass)

        logger.debug(
            "Analyzed ${irClass.name.asString()}: " +
                "${declaredProps.size + inheritedProps.size} properties, " +
                "${declaredFuncs.size + inheritedFuncs.size} functions",
        )

        return InterfaceAnalysis(
            interfaceName = interfaceName,
            typeParameters = typeParameters,
            properties = declaredProps + inheritedProps,
            functions = declaredFuncs + inheritedFuncs,
            sourceInterface = irClass,
            genericPattern = genericPattern,
        )
    }

    /**
     * Analyzes a property to extract metadata.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun analyzeProperty(irProp: IrProperty): PropertyAnalysis {
        val type = irProp.getter?.returnType ?: irProp.backingField?.type!!
        return PropertyAnalysis(
            name = irProp.name.asString(),
            type = type,
            isMutable = irProp.isVar,
            isNullable = type.isNullable(),
            irProperty = irProp,
        )
    }

    /**
     * Analyzes a function to extract metadata.
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun analyzeFunction(irFunc: IrSimpleFunction): FunctionAnalysis {
        // Extract extension receiver (if any)
        val extensionReceiver =
            irFunc.parameters
                .firstOrNull { it.kind == IrParameterKind.ExtensionReceiver }

        // Extract regular value parameters (filter out extension receiver)
        val parameters =
            irFunc.parameters
                .filter {
                    it.kind == IrParameterKind.Regular ||
                        it.kind == IrParameterKind.Context
                }
                .map { param ->
                    ParameterAnalysis(
                        name = param.name.asString(),
                        type = param.type,
                        hasDefaultValue = param.defaultValue != null,
                        defaultValueCode = null, // Not extracting default value code from compiled classes
                        isVararg = param.varargElementType != null,
                    )
                }

        val typeParams = irFunc.typeParameters.map { formatTypeParameter(it) }
        val typeParamBounds = extractBounds(irFunc.typeParameters)

        return FunctionAnalysis(
            name = irFunc.name.asString(),
            parameters = parameters,
            returnType = irFunc.returnType,
            isSuspend = irFunc.isSuspend,
            isInline = irFunc.isInline,
            typeParameters = typeParams,
            typeParameterBounds = typeParamBounds,
            isOperator = irFunc.isOperator,
            extensionReceiverType = extensionReceiver?.type,
            irFunction = irFunc,
        )
    }

    /**
     * Extracts inherited members from super-interfaces recursively.
     *
     * @param irClass The interface to analyze
     * @return Pair of (inherited properties, inherited functions)
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun extractInheritedMembers(
        irClass: IrClass,
    ): Pair<List<PropertyAnalysis>, List<FunctionAnalysis>> {
        val inheritedProps = mutableListOf<PropertyAnalysis>()
        val inheritedFuncs = mutableListOf<FunctionAnalysis>()

        // Walk super-interfaces recursively
        irClass.superTypes.forEach { superType ->
            // Skip Any/Object
            if (superType.isAny()) return@forEach

            val superClass = superType.classifierOrNull?.owner as? IrClass ?: return@forEach

            // Extract super properties
            superClass.declarations
                .filterIsInstance<IrProperty>()
                .forEach { inheritedProps += analyzeProperty(it) }

            // Extract super functions (filter synthetic)
            superClass.declarations
                .filterIsInstance<IrSimpleFunction>()
                .filterNot { it.isSynthetic() }
                .forEach { inheritedFuncs += analyzeFunction(it) }

            // Recurse
            val (nestedProps, nestedFuncs) = extractInheritedMembers(superClass)
            inheritedProps += nestedProps
            inheritedFuncs += nestedFuncs
        }

        return inheritedProps to inheritedFuncs
    }

    /**
     * Formats a type parameter with its bounds.
     *
     * Examples:
     * - T → "T"
     * - T : Comparable<T> → "T : Comparable<T>"
     * - K : Number → "K : Number"
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun formatTypeParameter(typeParam: IrTypeParameter): String {
        val bounds = typeParam.superTypes.filterNot { it.isAny() }
        return if (bounds.isEmpty()) {
            typeParam.name.asString()
        } else {
            val boundsStr = bounds.joinToString(", ") { typeResolver.irTypeToKotlinString(it, preserveTypeParameters = true) }
            "${typeParam.name.asString()} : $boundsStr"
        }
    }

    /**
     * Extracts type parameter bounds as a map.
     *
     * @return Map of type parameter name to bounds string (e.g., "T" to "Comparable<T>")
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun extractBounds(typeParams: List<IrTypeParameter>): Map<String, String> =
        typeParams
            .filter { it.superTypes.any { bound -> !bound.isAny() } }
            .associate { typeParam ->
                val boundsStr =
                    typeParam.superTypes
                        .filterNot { it.isAny() }
                        .joinToString(", ") { typeResolver.irTypeToKotlinString(it, preserveTypeParameters = true) }
                typeParam.name.asString() to boundsStr
            }

    /**
     * Checks if a function is synthetic and should be filtered out.
     *
     * Filters:
     * - <init>, <clinit> constructors
     * - Fake overrides (inherited members that don't need implementation)
     * - Delegated members
     */
    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun IrSimpleFunction.isSynthetic(): Boolean =
        name.asString().startsWith("<") ||
            origin == IrDeclarationOrigin.FAKE_OVERRIDE ||
            origin == IrDeclarationOrigin.DELEGATED_MEMBER
}
