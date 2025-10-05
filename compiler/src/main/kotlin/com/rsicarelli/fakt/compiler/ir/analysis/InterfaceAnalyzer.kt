// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

/**
 * Analyzes interface structure to extract metadata for fake generation.
 *
 * This analyzer handles interface-specific analysis including generic pattern detection
 * and SAM interface validation. For class analysis, see ClassAnalyzer.
 *
 * Separated from the main generator to isolate analysis logic and make
 * it easier to test and maintain.
 *
 * @since 1.0.0
 */
internal class InterfaceAnalyzer {
    private val patternAnalyzer = GenericPatternAnalyzer()

    fun analyzeInterfaceDynamically(sourceInterface: IrClass): InterfaceAnalysis {
        val properties = mutableListOf<PropertyAnalysis>()
        val functions = mutableListOf<FunctionAnalysis>()

        // Extract type parameters from the interface with constraints
        val typeParameters =
            sourceInterface.typeParameters.map { typeParam ->
                IrAnalysisHelper.formatTypeParameterWithConstraints(typeParam)
            }

        // Analyze all declarations in the interface
        sourceInterface.declarations.forEach { declaration ->
            when (declaration) {
                is IrProperty -> {
                    // Skip if this property doesn't have a getter (shouldn't happen in interfaces)
                    if (declaration.getter != null) {
                        properties.add(IrAnalysisHelper.analyzeProperty(declaration))
                    }
                }

                is IrSimpleFunction -> {
                    // Skip special functions (equals, hashCode, toString) and compiler-generated
                    if (!IrAnalysisHelper.isSpecialFunction(declaration)) {
                        functions.add(IrAnalysisHelper.analyzeFunction(declaration))
                    }
                }
            }
        }

        // Analyze generic pattern for optimal code generation
        val genericPattern = patternAnalyzer.analyzeInterface(sourceInterface)

        return InterfaceAnalysis(
            interfaceName = sourceInterface.name.asString(),
            typeParameters = typeParameters,
            properties = properties,
            functions = functions,
            sourceInterface = sourceInterface,
            genericPattern = genericPattern,
            debugInfo = StringBuilder(),
        )
    }
}

/**
 * Complete analysis of an interface including all its members and metadata.
 */
data class InterfaceAnalysis(
    val interfaceName: String,
    val typeParameters: List<String>, // Interface-level type parameters like <T>, <K, V>
    val properties: List<PropertyAnalysis>,
    val functions: List<FunctionAnalysis>,
    val sourceInterface: IrClass,
    val genericPattern: GenericPattern, // Smart pattern analysis for optimal code generation
    val debugInfo: StringBuilder = StringBuilder(), // Debug information for troubleshooting
)
