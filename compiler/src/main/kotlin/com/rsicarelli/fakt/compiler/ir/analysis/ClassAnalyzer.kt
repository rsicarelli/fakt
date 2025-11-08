// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.getClass
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.name.FqName

/**
 * Analyzes class structure to extract metadata for fake generation.
 *
 * This analyzer handles final and abstract classes with overridable members (open/abstract).
 * It works similarly to InterfaceAnalyzer but specifically handles class-specific logic
 * like distinguishing between abstract and open methods.
 */
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
     * @return true if the class can be faked, false otherwise
     */
    fun IrClass.isFakableClass(): Boolean =
        kind == ClassKind.CLASS &&
            modality != Modality.SEALED &&
            hasFakeAnnotation() &&
            hasOverridableMembers()

    /**
     * Checks if a class has @Fake annotation.
     *
     * @return true if class has @Fake annotation, false otherwise
     */
    private fun IrClass.hasFakeAnnotation(): Boolean {
        return hasAnnotation(FqName("com.rsicarelli.fakt.Fake"))
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

        // Extract type parameters from the class with constraints
        val typeParameters =
            sourceClass.typeParameters.map { typeParam ->
                IrAnalysisHelper.formatTypeParameterWithConstraints(typeParam)
            }

        // Collect interface method names for detection
        val interfaceMethodNames = collectInterfaceMethodNames(sourceClass)

        // Analyze all declarations in the class
        sourceClass.declarations.forEach { declaration ->
            when (declaration) {
                is IrProperty ->
                    analyzePropertyDeclaration(
                        declaration,
                        abstractProperties,
                        openProperties,
                    )
                is IrSimpleFunction ->
                    analyzeFunctionDeclaration(
                        declaration,
                        abstractMethods,
                        openMethods,
                        interfaceMethodNames,
                    )
            }
        }

        return ClassAnalysis(
            className = sourceClass.name.asString(),
            typeParameters = typeParameters,
            abstractMethods = abstractMethods,
            openMethods = openMethods,
            abstractProperties = abstractProperties,
            openProperties = openProperties,
            sourceClass = sourceClass,
        )
    }

    /**
     * Collects method names from all interfaces that this class implements.
     * Used as fallback for detecting interface methods when overriddenSymbols is unreliable.
     */
    private fun collectInterfaceMethodNames(sourceClass: IrClass): Set<String> {
        val interfaceMethodNames = mutableSetOf<String>()

        // Walk through all supertypes to find interfaces
        sourceClass.superTypes.forEach { superType ->
            val superClass = superType.getClass() ?: return@forEach
            if (superClass.kind == ClassKind.INTERFACE) {
                // Collect all method names from this interface
                superClass.declarations.filterIsInstance<IrSimpleFunction>().forEach { function ->
                    if (!IrAnalysisHelper.isSpecialFunction(function)) {
                        interfaceMethodNames.add(function.name.asString())
                    }
                }
            }
        }

        return interfaceMethodNames
    }

    private fun analyzePropertyDeclaration(
        declaration: IrProperty,
        abstractProperties: MutableList<PropertyAnalysis>,
        openProperties: MutableList<PropertyAnalysis>,
    ) {
        // Skip compiler-generated properties
        if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return

        // For properties with custom getters, check the getter's modality
        val effectiveModality = declaration.getter?.modality ?: declaration.modality

        when (effectiveModality) {
            Modality.ABSTRACT -> {
                abstractProperties.add(IrAnalysisHelper.analyzeProperty(declaration))
            }
            Modality.OPEN -> {
                openProperties.add(IrAnalysisHelper.analyzeProperty(declaration))
            }
            else -> { /* Skip final properties */ }
        }
    }

    private fun analyzeFunctionDeclaration(
        declaration: IrSimpleFunction,
        abstractMethods: MutableList<FunctionAnalysis>,
        openMethods: MutableList<FunctionAnalysis>,
        interfaceMethodNames: Set<String>,
    ) {
        // Skip special functions and compiler-generated
        if (IrAnalysisHelper.isSpecialFunction(declaration)) return
        if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return

        // Check if this method overrides an abstract method from superclass or interface
        // Interface methods should always require configuration (error defaults)
        val isOverridingAbstract =
            declaration.overriddenSymbols.any { overriddenSymbol ->
                overriddenSymbol.owner.modality == Modality.ABSTRACT
            }

        val isOverridingInterface =
            declaration.overriddenSymbols.any { overriddenSymbol ->
                val parentDeclaration = overriddenSymbol.owner.parent
                parentDeclaration is IrClass && parentDeclaration.kind == ClassKind.INTERFACE
            }

        // Fallback: Check if method name matches any interface method
        // This handles cases where overriddenSymbols doesn't contain interface methods
        val isLikelyFromInterface = interfaceMethodNames.contains(declaration.name.asString())

        when {
            // Priority 1: Methods overriding abstract/interface methods → error() defaults
            isOverridingAbstract || isOverridingInterface || isLikelyFromInterface -> {
                abstractMethods.add(IrAnalysisHelper.analyzeFunction(declaration))
            }
            // Priority 2: Methods declared as abstract in this class → error() defaults
            declaration.modality == Modality.ABSTRACT -> {
                abstractMethods.add(IrAnalysisHelper.analyzeFunction(declaration))
            }
            // Priority 3: Open methods without abstract/interface override → super call defaults
            declaration.modality == Modality.OPEN -> {
                openMethods.add(IrAnalysisHelper.analyzeFunction(declaration))
            }
            else -> { /* Skip final methods */ }
        }
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
    val typeParameters: List<String>, // Class-level type parameters like <T>, <K, V>
    val abstractMethods: List<FunctionAnalysis>, // Abstract methods (must be overridden)
    val openMethods: List<FunctionAnalysis>, // Open methods (can be overridden, default to super)
    val abstractProperties: List<PropertyAnalysis>, // Abstract properties (must be overridden)
    val openProperties: List<PropertyAnalysis>, // Open properties (can be overridden, default to super)
    val sourceClass: IrClass,
)
