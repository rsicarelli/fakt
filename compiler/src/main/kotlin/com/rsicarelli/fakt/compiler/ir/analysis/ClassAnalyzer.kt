// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

/**
 * Analyzes class structure to extract metadata for fake generation.
 *
 * This analyzer handles final and abstract classes with overridable members (open/abstract).
 * It works similarly to InterfaceAnalyzer but specifically handles class-specific logic
 * like distinguishing between abstract and open methods.
 *
 * @since 1.0.0
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
@Suppress("DEPRECATION")
internal object ClassAnalyzer {
    /**
     * Checks if a class can be faked (final or abstract classes with overridable members).
     *
     * A class is fakable if:
     * 1. It's a CLASS (not interface, object, enum)
     * 2. It's NOT sealed
     * 3. It has @Fake annotation
     * 4. It has at least one open/abstract method or property
     *
     * @param irClass The class to check
     * @return true if the class can be faked, false otherwise
     */
    fun IrClass.isFakableClass(): Boolean {
        // Check 1: Must be a class (not interface, object, enum)
        if (kind != ClassKind.CLASS) return false

        // Check 2: Must not be sealed (use sealed hierarchy support later)
        if (modality == Modality.SEALED) return false

        // Check 3: Must have @Fake annotation
        if (!hasAnnotation(FqName("com.rsicarelli.fakt.Fake"))) return false

        // Check 4: Must have at least one open/abstract method or property
        if (!hasOverridableMembers()) return false

        return true
    }

    /**
     * Checks if a class has any overridable members (open or abstract methods/properties).
     *
     * @return true if class has at least one overridable member
     */
    private fun IrClass.hasOverridableMembers(): Boolean {
        return declarations.any { declaration ->
            when (declaration) {
                is IrSimpleFunction -> {
                    // Skip compiler-generated and special functions
                    if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return@any false
                    if (IrAnalysisHelper.isSpecialFunction(declaration)) return@any false

                    // Check if function is overridable (open or abstract)
                    declaration.modality == Modality.OPEN ||
                        declaration.modality == Modality.ABSTRACT
                }
                is IrProperty -> {
                    // Check if property is overridable (open or abstract)
                    declaration.modality == Modality.OPEN ||
                        declaration.modality == Modality.ABSTRACT
                }
                else -> false
            }
        }
    }

    /**
     * Analyzes a class to extract abstract and open members for fake generation.
     *
     * Similar to analyzeInterfaceDynamically but handles classes (abstract/final with open methods).
     * Extracts both abstract members (must override) and open members (optional override).
     *
     * @param sourceClass The class to analyze
     * @return Class analysis with abstract and open members
     */
    fun analyzeClass(sourceClass: IrClass): ClassAnalysis {
        val abstractMethods = mutableListOf<FunctionAnalysis>()
        val openMethods = mutableListOf<FunctionAnalysis>()
        val abstractProperties = mutableListOf<PropertyAnalysis>()
        val openProperties = mutableListOf<PropertyAnalysis>()

        // Analyze all declarations in the class
        sourceClass.declarations.forEach { declaration ->
            when (declaration) {
                is IrProperty -> {
                    // Skip compiler-generated properties
                    if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return@forEach

                    when (declaration.modality) {
                        Modality.ABSTRACT -> {
                            abstractProperties.add(IrAnalysisHelper.analyzeProperty(declaration))
                        }
                        Modality.OPEN -> {
                            openProperties.add(IrAnalysisHelper.analyzeProperty(declaration))
                        }
                        else -> { /* Skip final properties */ }
                    }
                }

                is IrSimpleFunction -> {
                    // Skip special functions and compiler-generated
                    if (IrAnalysisHelper.isSpecialFunction(declaration)) return@forEach
                    if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return@forEach

                    when (declaration.modality) {
                        Modality.ABSTRACT -> {
                            abstractMethods.add(IrAnalysisHelper.analyzeFunction(declaration))
                        }
                        Modality.OPEN -> {
                            openMethods.add(IrAnalysisHelper.analyzeFunction(declaration))
                        }
                        else -> { /* Skip final methods */ }
                    }
                }
            }
        }

        return ClassAnalysis(
            className = sourceClass.name.asString(),
            abstractMethods = abstractMethods,
            openMethods = openMethods,
            abstractProperties = abstractProperties,
            openProperties = openProperties,
            sourceClass = sourceClass,
        )
    }
}

/**
 * Complete analysis of a class (final or abstract) for fake generation.
 *
 * Classes can have both abstract members (must override) and open members (optional override).
 * Abstract members require configuration, while open members default to calling super.
 */
data class ClassAnalysis(
    val className: String,
    val abstractMethods: List<FunctionAnalysis>, // Abstract methods (must be overridden)
    val openMethods: List<FunctionAnalysis>, // Open methods (can be overridden, default to super)
    val abstractProperties: List<PropertyAnalysis>, // Abstract properties (must be overridden)
    val openProperties: List<PropertyAnalysis>, // Open properties (can be overridden, default to super)
    val sourceClass: IrClass,
)
