// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.generation

import com.rsicarelli.fakt.codegen.analysis.ConstructorParam
import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.codegen.analysis.FunctionSpec
import com.rsicarelli.fakt.codegen.analysis.ParameterSpec
import com.rsicarelli.fakt.codegen.analysis.PropertySpec
import com.rsicarelli.fakt.codegen.analysis.PureGenericPattern
import com.rsicarelli.fakt.compiler.fir.metadata.FirFunctionInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirPropertyInfo
import com.rsicarelli.fakt.compiler.fir.metadata.FirRenderedType
import com.rsicarelli.fakt.compiler.fir.metadata.FirTypeParameterInfo
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeClass
import com.rsicarelli.fakt.compiler.fir.metadata.ValidatedFakeInterface
import com.rsicarelli.fakt.compiler.fir.metadata.resolveCallHistory
import com.rsicarelli.fakt.compiler.fir.metadata.resolveMutability
import com.rsicarelli.fakt.compiler.fir.rendering.formatTypeParameter
import com.rsicarelli.fakt.compiler.fir.rendering.toDeclarationAnnotation

/*
 * FIR → FakeDeclaration translator.
 *
 * The FIR-phase counterpart of `IrToFakeDeclarationTranslator`: builds the pure `FakeDeclaration`
 * contract directly from the checker-extracted metadata, consuming the `FirRenderedType`
 * side-channels captured at analysis time (`FirTypeRenderer`) instead of resolving `IrType`s.
 * Field-for-field parity with the IR translator is locked by `FirIrEmissionParityTest`.
 */

/**
 * Builds a [FakeDeclaration.Interface] from checker-extracted metadata.
 *
 * @param enableCallHistoryDefault Plugin-level default applied when the `@Fake` annotation's
 *   `callHistory` is `DEFAULT`.
 * @param enableMutableFakesDefault Plugin-level default applied when the `@Fake` annotation's
 *   `mutability` is `DEFAULT`.
 */
internal fun ValidatedFakeInterface.toFakeInterface(
    enableCallHistoryDefault: Boolean = true,
    enableMutableFakesDefault: Boolean = false,
): FakeDeclaration.Interface {
    // Declared-then-inherited ordering matches the IR path (FirToIrTransformer.transform)
    val allProperties = properties + inheritedProperties
    val allFunctions = functions + inheritedFunctions

    return FakeDeclaration.Interface(
        simpleName = simpleName,
        qualifiedSourceName = qualifiedSourceName,
        packageName = packageName,
        typeParameters = typeParameters.map { formatTypeParameter(it) },
        visibility = visibility,
        annotations = annotations.map { it.toDeclarationAnnotation() },
        requiredImports = collectFqns(allProperties, allFunctions),
        generateCallHistory = callHistoryMode.resolveCallHistory(enableCallHistoryDefault),
        generateMutableBehaviors = mutabilityMode.resolveMutability(enableMutableFakesDefault),
        genericPattern = derivePureGenericPattern(typeParameters, functions),
        properties = allProperties.map { it.toPropertySpec() },
        functions = allFunctions.map { it.toFunctionSpec() },
    )
}

/** Builds a [FakeDeclaration.Class] from checker-extracted metadata. */
internal fun ValidatedFakeClass.toFakeClass(
    enableCallHistoryDefault: Boolean = true,
    enableMutableFakesDefault: Boolean = false,
): FakeDeclaration.Class =
    FakeDeclaration.Class(
        simpleName = simpleName,
        qualifiedSourceName = qualifiedSourceName,
        packageName = packageName,
        typeParameters = typeParameters.map { formatTypeParameter(it) },
        visibility = visibility,
        annotations = annotations.map { it.toDeclarationAnnotation() },
        requiredImports =
            collectFqns(abstractProperties + openProperties, abstractMethods + openMethods),
        generateCallHistory = callHistoryMode.resolveCallHistory(enableCallHistoryDefault),
        generateMutableBehaviors = mutabilityMode.resolveMutability(enableMutableFakesDefault),
        constructorParameters =
            constructorParameters.map { param ->
                ConstructorParam(
                    name = param.name,
                    typeString = param.rendered.shortName,
                    hasDefault = param.hasDefault,
                )
            },
        abstractMethods = abstractMethods.map { it.toFunctionSpec() },
        openMethods = openMethods.map { it.toFunctionSpec() },
        abstractProperties = abstractProperties.map { it.toPropertySpec() },
        openProperties = openProperties.map { it.toPropertySpec() },
    )

private fun FirPropertyInfo.toPropertySpec(): PropertySpec {
    val renderedType = requireRendered(rendered, "property '$name'")
    return PropertySpec(
        name = name,
        typeString = renderedType.shortName,
        isMutable = isMutable,
        isNullable = isNullable,
        isTypeParameter = renderedType.isTypeParameter,
        requiresCollectionErasure = renderedType.requiresCollectionErasure,
    )
}

private fun FirFunctionInfo.toFunctionSpec(): FunctionSpec =
    FunctionSpec(
        name = name,
        parameters = parameters.map { it.toParameterSpec(functionName = name) },
        returnTypeString = requireRendered(renderedReturnType, "return of '$name'").shortName,
        extensionReceiverTypeString = extensionReceiverRendered?.shortName,
        isSuspend = isSuspend,
        isInline = isInline,
        isOperator = isOperator,
        typeParameters = typeParameters.map { formatTypeParameter(it) },
        typeParameterBounds = typeParameterBounds,
    )

private fun FirParameterInfo.toParameterSpec(functionName: String): ParameterSpec {
    val renderedType = requireRendered(rendered, "parameter '$name' of '$functionName'")
    return ParameterSpec(
        name = name,
        typeString = renderedType.shortName,
        hasDefaultValue = hasDefaultValue,
        defaultValueCode = defaultValueCode,
        isVararg = isVararg,
        isTypeParameter = renderedType.isTypeParameter,
        requiresCollectionErasure = renderedType.requiresCollectionErasure,
    )
}

private fun requireRendered(rendered: FirRenderedType?, what: String): FirRenderedType =
    rendered
        ?: error(
            "FIR-rendered type missing for $what. FIR-phase emission requires the " +
                "FirRenderedType side-channels captured during checker analysis."
        )

/**
 * Derives the pure generic pattern from FIR metadata.
 *
 * Mirrors `GenericPatternAnalyzer.analyzeInterface`, which classifies from the class's own
 * declarations — inherited functions do not participate.
 */
private fun derivePureGenericPattern(
    classTypeParameters: List<FirTypeParameterInfo>,
    declaredFunctions: List<FirFunctionInfo>,
): PureGenericPattern {
    val genericMethodNames =
        declaredFunctions.filter { it.typeParameters.isNotEmpty() }.map { it.name }

    return when {
        classTypeParameters.isEmpty() && genericMethodNames.isEmpty() ->
            PureGenericPattern.NoGenerics
        classTypeParameters.isNotEmpty() && genericMethodNames.isEmpty() ->
            PureGenericPattern.ClassLevelGenerics(
                typeParameterNames = classTypeParameters.map { it.name }
            )
        classTypeParameters.isEmpty() ->
            PureGenericPattern.MethodLevelGenerics(methodNames = genericMethodNames)
        else ->
            PureGenericPattern.MixedGenerics(
                typeParameterNames = classTypeParameters.map { it.name },
                methodNames = genericMethodNames,
            )
    }
}

/** Union of the rendered FQN side-channels — same harvesting as the IR translator. */
private fun collectFqns(
    properties: List<FirPropertyInfo>,
    functions: List<FirFunctionInfo>,
): Set<String> {
    val fqns = mutableSetOf<String>()
    properties.forEach { property -> property.rendered?.fqns?.let(fqns::addAll) }
    functions.forEach { function ->
        function.renderedReturnType?.fqns?.let(fqns::addAll)
        function.parameters.forEach { parameter -> parameter.rendered?.fqns?.let(fqns::addAll) }
    }
    return fqns
}
