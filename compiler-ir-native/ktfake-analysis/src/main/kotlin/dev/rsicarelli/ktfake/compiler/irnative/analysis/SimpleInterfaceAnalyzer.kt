// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.irnative.analysis

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Simple implementation of InterfaceAnalyzer for MVP.
 *
 * This version focuses on working functionality first, then we'll enhance with IR APIs.
 * Follows TDD approach - makes tests pass first.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class SimpleInterfaceAnalyzer : InterfaceAnalyzer {

    /**
     * Complete interface analysis with real IR inspection.
     */
    override fun analyzeInterface(sourceInterface: IrClass): InterfaceAnalysis {
        val interfaceName = sourceInterface.name.asString()
        val packageName = extractPackageName(sourceInterface)

        // Real method analysis
        val methods = analyzeInterfaceMethods(sourceInterface)

        // Real property analysis
        val properties = analyzeInterfaceProperties(sourceInterface)

        // Real generic analysis
        val generics = analyzeGenericParameters(sourceInterface)

        // Real annotation analysis
        val annotations = analyzeAnnotations(sourceInterface)

        return InterfaceAnalysis(
            sourceInterface = sourceInterface,
            interfaceName = interfaceName,
            packageName = packageName,
            methods = methods,
            properties = properties,
            generics = generics,
            annotations = annotations,
            dependencies = emptyList() // TODO: Implement dependency analysis
        )
    }

    /**
     * Simple fake interface discovery - MVP implementation.
     */
    override fun discoverFakeInterfaces(moduleClasses: List<IrClass>): List<IrClass> {
        return moduleClasses.filter { irClass ->
            irClass.kind == ClassKind.INTERFACE &&
            irClass.annotations.isNotEmpty()
        }
    }

    /**
     * Basic validation - MVP implementation.
     */
    override fun validateInterface(sourceInterface: IrClass): ValidationResult {
        val errors = mutableListOf<String>()

        // Rule 1: Objects not allowed
        if (sourceInterface.kind == ClassKind.OBJECT) {
            errors.add("@Fake cannot be applied to objects. Use interface or class for thread safety.")
        }

        // Rule 2: Must have some content (simplified check)
        val hasContent = sourceInterface.declarations.isNotEmpty()
        if (!hasContent) {
            errors.add("Interface '${sourceInterface.name}' has no methods or properties to fake")
        }

        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }

    /**
     * Extract package name from IR class.
     */
    private fun extractPackageName(sourceInterface: IrClass): String {
        return sourceInterface.parent.let { parent ->
            when (parent) {
                is IrPackageFragment -> parent.packageFqName.asString()
                else -> "" // Fallback for complex hierarchies
            }
        }
    }

    /**
     * Analyze all methods declared in the interface.
     */
    private fun analyzeInterfaceMethods(sourceInterface: IrClass): List<MethodAnalysis> {
        return sourceInterface.declarations
            .filterIsInstance<IrSimpleFunction>()
            .filter { function ->
                // Only analyze abstract methods (interface methods)
                function.modality == org.jetbrains.kotlin.descriptors.Modality.ABSTRACT
            }
            .map { function ->
                analyzeMethod(function)
            }
    }

    /**
     * Analyze all properties declared in the interface.
     */
    private fun analyzeInterfaceProperties(sourceInterface: IrClass): List<PropertyAnalysis> {
        return sourceInterface.declarations
            .filterIsInstance<IrProperty>()
            .map { property ->
                analyzeProperty(property)
            }
    }

    /**
     * Analyze generic type parameters.
     */
    private fun analyzeGenericParameters(sourceInterface: IrClass): List<GenericAnalysis> {
        return sourceInterface.typeParameters.map { typeParam ->
            GenericAnalysis(
                name = typeParam.name.asString(),
                bounds = typeParam.superTypes.map { superType ->
                    analyzeType(superType)
                },
                variance = when (typeParam.variance) {
                    org.jetbrains.kotlin.types.Variance.IN_VARIANCE -> GenericVariance.CONTRAVARIANT
                    org.jetbrains.kotlin.types.Variance.OUT_VARIANCE -> GenericVariance.COVARIANT
                    else -> GenericVariance.INVARIANT
                }
            )
        }
    }

    /**
     * Analyze @Fake annotation and its parameters.
     */
    private fun analyzeAnnotations(sourceInterface: IrClass): AnnotationAnalysis {
        val fakeAnnotation = sourceInterface.annotations.find { annotation ->
            annotation.type.classFqName?.asString() == "dev.rsicarelli.ktfake.Fake"
        }

        return if (fakeAnnotation != null) {
            // TODO: Extract annotation parameters (trackCalls, builder, etc.)
            AnnotationAnalysis(
                trackCalls = false, // Extract from annotation
                builder = false,    // Extract from annotation
                concurrent = true,  // Extract from annotation
                scope = "test",     // Extract from annotation
                dependencies = emptyList() // Extract from annotation
            )
        } else {
            // Default annotation analysis
            AnnotationAnalysis(
                trackCalls = false,
                builder = false,
                concurrent = true,
                scope = "test",
                dependencies = emptyList()
            )
        }
    }

    /**
     * Analyze individual method signature and properties.
     */
    private fun analyzeMethod(function: IrSimpleFunction): MethodAnalysis {
        val parameters = function.valueParameters.map { param ->
            ParameterAnalysis(
                name = param.name.asString(),
                type = analyzeType(param.type),
                hasDefaultValue = param.defaultValue != null,
                isVararg = param.varargElementType != null
            )
        }

        val returnType = analyzeType(function.returnType)

        val modifiers = mutableSetOf<MethodModifier>()
        if (function.isSuspend) modifiers.add(MethodModifier.SUSPEND)
        if (function.modality == org.jetbrains.kotlin.descriptors.Modality.ABSTRACT) {
            modifiers.add(MethodModifier.ABSTRACT)
        }

        return MethodAnalysis(
            function = function,
            name = function.name.asString(),
            parameters = parameters,
            returnType = returnType,
            isSuspend = function.isSuspend,
            modifiers = modifiers
        )
    }

    /**
     * Analyze individual property signature and type.
     */
    private fun analyzeProperty(property: IrProperty): PropertyAnalysis {
        return PropertyAnalysis(
            property = property,
            name = property.name.asString(),
            type = analyzeType(property.getter?.returnType ?: property.backingField?.type!!),
            hasGetter = property.getter != null,
            hasSetter = property.setter != null,
            modifiers = setOf() // TODO: Extract property modifiers
        )
    }

    /**
     * Analyze IR type and convert to analysis format.
     */
    private fun analyzeType(irType: IrType): TypeAnalysis {
        val qualifiedName = when {
            irType.isString() -> "kotlin.String"
            irType.isInt() -> "kotlin.Int"
            irType.isBoolean() -> "kotlin.Boolean"
            irType.isUnit() -> "kotlin.Unit"
            irType.isLong() -> "kotlin.Long"
            irType.isFloat() -> "kotlin.Float"
            irType.isDouble() -> "kotlin.Double"
            irType.isAny() -> "kotlin.Any"
            else -> {
                // Get the class FQ name if possible
                val classifier = irType.classifierOrNull
                classifier?.owner?.let { owner ->
                    when (owner) {
                        is IrClass -> owner.fqNameWhenAvailable?.asString() ?: owner.name.asString()
                        else -> owner.toString()
                    }
                } ?: "kotlin.Any"
            }
        }

        return TypeAnalysis(
            qualifiedName = qualifiedName,
            isNullable = irType.isMarkedNullable(),
            genericArguments = extractGenericArguments(irType),
            isBuiltin = qualifiedName.startsWith("kotlin.")
        )
    }

    /**
     * Extract generic type arguments from IR type.
     */
    private fun extractGenericArguments(irType: IrType): List<TypeAnalysis> {
        return if (irType is IrSimpleType) {
            irType.arguments.mapNotNull { argument ->
                when (argument) {
                    is IrTypeProjection -> analyzeType(argument.type)
                    else -> null
                }
            }
        } else {
            emptyList()
        }
    }
}
