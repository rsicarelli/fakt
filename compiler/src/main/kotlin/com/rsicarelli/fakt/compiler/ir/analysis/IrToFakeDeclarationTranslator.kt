// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.analysis

import com.rsicarelli.fakt.codegen.analysis.ConstructorParam
import com.rsicarelli.fakt.codegen.analysis.DeclarationAnnotation
import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.codegen.analysis.FunctionSpec
import com.rsicarelli.fakt.codegen.analysis.ParameterSpec
import com.rsicarelli.fakt.codegen.analysis.PropertySpec
import com.rsicarelli.fakt.codegen.analysis.PureGenericPattern
import com.rsicarelli.fakt.compiler.core.types.TypeResolution
import com.rsicarelli.fakt.compiler.fir.metadata.FirCallHistoryMode
import com.rsicarelli.fakt.compiler.fir.metadata.FirMutabilityMode
import com.rsicarelli.fakt.compiler.ir.transform.IrAnnotationMetadata
import com.rsicarelli.fakt.compiler.ir.transform.IrClassGenerationMetadata
import com.rsicarelli.fakt.compiler.ir.transform.IrFunctionMetadata
import com.rsicarelli.fakt.compiler.ir.transform.IrGenerationMetadata
import com.rsicarelli.fakt.compiler.ir.transform.IrParameterMetadata
import com.rsicarelli.fakt.compiler.ir.transform.IrPropertyMetadata
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/*
 * IR → FakeDeclaration translator.
 *
 * Bridges the IR-side metadata (which carries `IrType`/`IrClass` references) to the pure
 * `FakeDeclaration` contract that lives in `:codegen-runtime`. This is the single point where
 * `IrType` rendering happens; per-member helpers reuse the pre-populated `RenderedType` side
 * channel when available, falling back to a fresh `TypeResolution.irTypeToKotlinString` call
 * otherwise.
 */

/**
 * Builds a [FakeDeclaration.Interface] from [IrGenerationMetadata]. Per-member helpers reuse
 * pre-populated [com.rsicarelli.fakt.compiler.core.types.RenderedType] side channels when present
 * and fall back to [typeResolver] otherwise.
 *
 * @param enableCallHistoryDefault Plugin-level default applied when the `@Fake` annotation's
 *   `callHistory` is `DEFAULT`.
 * @param enableMutableFakesDefault Plugin-level default applied when the `@Fake` annotation's
 *   `mutability` is `DEFAULT`.
 */
internal fun IrGenerationMetadata.toFakeInterface(
    typeResolver: TypeResolution,
    enableCallHistoryDefault: Boolean = true,
    enableMutableFakesDefault: Boolean = false,
): FakeDeclaration.Interface =
    FakeDeclaration.Interface(
        simpleName = interfaceName,
        qualifiedSourceName = qualifiedSourceName,
        packageName = packageName,
        typeParameters = typeParameters,
        visibility = visibility,
        annotations = annotations.map { it.toDeclarationAnnotation() },
        requiredImports = collectFqns(properties, functions),
        generateCallHistory = callHistoryMode.resolveCallHistory(enableCallHistoryDefault),
        generateMutableBehaviors = mutabilityMode.resolveMutability(enableMutableFakesDefault),
        genericPattern = genericPattern.toPureGenericPattern(),
        properties = properties.map { it.toPropertySpec(typeResolver) },
        functions = functions.map { it.toFunctionSpec(typeResolver) },
    )

/**
 * Builds a [FakeDeclaration.Class] from [IrClassGenerationMetadata]. Constructor parameters are
 * extracted from the source class's primary constructor.
 */
internal fun IrClassGenerationMetadata.toFakeClass(
    typeResolver: TypeResolution,
    enableCallHistoryDefault: Boolean = true,
    enableMutableFakesDefault: Boolean = false,
): FakeDeclaration.Class =
    FakeDeclaration.Class(
        simpleName = className,
        qualifiedSourceName = qualifiedSourceName,
        packageName = packageName,
        typeParameters = typeParameters,
        visibility = visibility,
        annotations = annotations.map { it.toDeclarationAnnotation() },
        requiredImports =
            collectFqns(abstractProperties + openProperties, abstractMethods + openMethods),
        generateCallHistory = callHistoryMode.resolveCallHistory(enableCallHistoryDefault),
        generateMutableBehaviors = mutabilityMode.resolveMutability(enableMutableFakesDefault),
        constructorParameters = sourceClass.extractConstructorParams(typeResolver),
        abstractMethods = abstractMethods.map { it.toFunctionSpec(typeResolver) },
        openMethods = openMethods.map { it.toFunctionSpec(typeResolver) },
        abstractProperties = abstractProperties.map { it.toPropertySpec(typeResolver) },
        openProperties = openProperties.map { it.toPropertySpec(typeResolver) },
    )

@OptIn(UnsafeDuringIrConstructionAPI::class)
private fun IrClass.extractConstructorParams(typeResolver: TypeResolution): List<ConstructorParam> {
    val constructor = declarations.filterIsInstance<IrConstructor>().firstOrNull { it.isPrimary }
    return constructor
        ?.parameters
        .orEmpty()
        .filter { it.kind == IrParameterKind.Regular }
        .map { param ->
            ConstructorParam(
                name = param.name.asString(),
                typeString =
                    typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true),
                hasDefault = param.defaultValue != null,
            )
        }
}

private fun IrPropertyMetadata.toPropertySpec(typeResolver: TypeResolution): PropertySpec =
    PropertySpec(
        name = name,
        typeString =
            renderedType?.shortName
                ?: typeResolver.irTypeToKotlinString(type, preserveTypeParameters = true),
        isMutable = isMutable,
        isNullable = isNullable,
        isTypeParameter = isTypeParameter,
        requiresCollectionErasure = requiresCollectionErasure,
    )

private fun IrFunctionMetadata.toFunctionSpec(typeResolver: TypeResolution): FunctionSpec =
    FunctionSpec(
        name = name,
        parameters = parameters.map { it.toParameterSpec(typeResolver) },
        returnTypeString =
            renderedReturnType?.shortName
                ?: typeResolver.irTypeToKotlinString(returnType, preserveTypeParameters = true),
        extensionReceiverTypeString =
            extensionReceiverType?.let {
                typeResolver.irTypeToKotlinString(it, preserveTypeParameters = true)
            },
        isSuspend = isSuspend,
        isInline = isInline,
        isOperator = isOperator,
        typeParameters = typeParameters,
        typeParameterBounds = typeParameterBounds,
    )

private fun IrParameterMetadata.toParameterSpec(typeResolver: TypeResolution): ParameterSpec =
    ParameterSpec(
        name = name,
        typeString =
            renderedType?.shortName
                ?: typeResolver.irTypeToKotlinString(type, preserveTypeParameters = true),
        hasDefaultValue = hasDefaultValue,
        defaultValueCode = defaultValueCode,
        isVararg = isVararg,
        isTypeParameter = isTypeParameter,
        requiresCollectionErasure = requiresCollectionErasure,
    )

private fun IrAnnotationMetadata.toDeclarationAnnotation(): DeclarationAnnotation =
    DeclarationAnnotation(
        simpleName = simpleName,
        fullyQualifiedName = fullyQualifiedName,
        renderedArguments = renderedArguments,
        isOptInMarker = isOptInMarker,
    )

private fun GenericPattern.toPureGenericPattern(): PureGenericPattern =
    when (this) {
        is GenericPattern.NoGenerics -> PureGenericPattern.NoGenerics
        is GenericPattern.ClassLevelGenerics ->
            PureGenericPattern.ClassLevelGenerics(
                typeParameterNames = typeParameters.map { it.name.asString() }
            )
        is GenericPattern.MethodLevelGenerics ->
            PureGenericPattern.MethodLevelGenerics(methodNames = genericMethods.map { it.name })
        is GenericPattern.MixedGenerics ->
            PureGenericPattern.MixedGenerics(
                typeParameterNames = classTypeParameters.map { it.name.asString() },
                methodNames = genericMethods.map { it.name },
            )
    }

/**
 * Resolves the `@Fake` annotation's call-history mode against the plugin-level default. ENABLED and
 * DISABLED override [pluginDefault]; DEFAULT follows it.
 */
internal fun FirCallHistoryMode.resolveCallHistory(pluginDefault: Boolean): Boolean =
    when (this) {
        FirCallHistoryMode.ENABLED -> true
        FirCallHistoryMode.DISABLED -> false
        FirCallHistoryMode.DEFAULT -> pluginDefault
    }

/**
 * Resolves the `@Fake` annotation's mutability mode against the plugin-level default. MUTABLE and
 * IMMUTABLE override [pluginDefault]; DEFAULT follows it.
 */
internal fun FirMutabilityMode.resolveMutability(pluginDefault: Boolean): Boolean =
    when (this) {
        FirMutabilityMode.MUTABLE -> true
        FirMutabilityMode.IMMUTABLE -> false
        FirMutabilityMode.DEFAULT -> pluginDefault
    }

private fun collectFqns(
    properties: List<IrPropertyMetadata>,
    functions: List<IrFunctionMetadata>,
): Set<String> {
    val fqns = mutableSetOf<String>()
    properties.forEach { property -> property.renderedType?.fqns?.let(fqns::addAll) }
    functions.forEach { function ->
        function.renderedReturnType?.fqns?.let(fqns::addAll)
        function.parameters.forEach { parameter -> parameter.renderedType?.fqns?.let(fqns::addAll) }
    }
    return fqns
}
