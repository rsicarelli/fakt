// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.classifierOrNull
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
internal object ClassAnalyzer {
    /**
     * Checks if a class can be faked (final or abstract classes with overridable members).
     *
     * A class is fakable if:
     * 1. It's a CLASS (not interface, object, enum)
     * 2. It's NOT sealed
     * 3. It has @Fake annotation OR an annotation marked with @GeneratesFake
     * 4. It has at least one open/abstract method or property
     *
     * @return true if the class can be faked, false otherwise
     */
    fun IrClass.isFakableClass(): Boolean {
        // Check 1: Must be a class (not interface, object, enum)
        if (kind != ClassKind.CLASS) return false

        // Check 2: Must not be sealed (use sealed hierarchy support later)
        if (modality == Modality.SEALED) return false

        // Check 3: Must have @Fake annotation OR annotation with @GeneratesFake meta-annotation
        if (!hasFakeAnnotation()) return false

        // Check 4: Must have at least one open/abstract method or property
        if (!hasOverridableMembers()) return false

        return true
    }

    /**
     * Checks if a class has a fake generation annotation.
     *
     * This method checks for annotations in two ways:
     * 1. Direct @Fake annotation (backward compatibility)
     * 2. Any annotation marked with @GeneratesFake meta-annotation
     *
     * @return true if class has a fake annotation, false otherwise
     */
    private fun IrClass.hasFakeAnnotation(): Boolean {
        // Direct @Fake check
        if (hasAnnotation(FqName("com.rsicarelli.fakt.Fake"))) return true

        // Check for @GeneratesFake meta-annotation
        return annotations.any { annotation ->
            hasGeneratesFakeMetaAnnotation(annotation)
        }
    }

    /**
     * Checks if an annotation is annotated with @GeneratesFake meta-annotation.
     *
     * This enables companies to define their own annotations (like @TestDouble)
     * by marking them with @GeneratesFake, without being locked into @Fake.
     *
     * Pattern inspired by Kotlin's @HidesFromObjC meta-annotation.
     *
     * @param annotation The annotation to check
     * @return true if the annotation has @GeneratesFake meta-annotation, false otherwise
     */
    private fun hasGeneratesFakeMetaAnnotation(annotation: org.jetbrains.kotlin.ir.expressions.IrConstructorCall): Boolean {
        try {
            // Get the annotation class from the type
            val annotationType = annotation.type
            val annotationClassSymbol = annotationType.classifierOrNull ?: return false
            val annotationClass = annotationClassSymbol.owner as? IrClass ?: return false

            // Check if the annotation class itself has @GeneratesFake annotation
            return annotationClass.annotations.any { metaAnnotation ->
                metaAnnotation.type.classFqName?.asString() == "com.rsicarelli.fakt.GeneratesFake"
            }
        } catch (e: Exception) {
            // Safely handle any IR traversal errors
            // This is expected for some annotation patterns
            return false
        }
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
    ) {
        // Skip special functions and compiler-generated
        if (IrAnalysisHelper.isSpecialFunction(declaration)) return
        if (declaration.origin == IrDeclarationOrigin.FAKE_OVERRIDE) return

        // Check if this method overrides an abstract method from superclass
        val isOverridingAbstract =
            declaration.overriddenSymbols.any { overriddenSymbol ->
                overriddenSymbol.owner.modality == Modality.ABSTRACT
            }

        when {
            // Priority 1: Methods overriding abstract methods → error() defaults
            isOverridingAbstract -> {
                abstractMethods.add(IrAnalysisHelper.analyzeFunction(declaration))
            }
            // Priority 2: Methods declared as abstract in this class → error() defaults
            declaration.modality == Modality.ABSTRACT -> {
                abstractMethods.add(IrAnalysisHelper.analyzeFunction(declaration))
            }
            // Priority 3: Open methods without abstract override → super call defaults
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
