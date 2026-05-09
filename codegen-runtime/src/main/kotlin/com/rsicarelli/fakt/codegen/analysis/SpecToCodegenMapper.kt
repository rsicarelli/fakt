// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.analysis

import com.rsicarelli.fakt.codegen.builder.parseType
import com.rsicarelli.fakt.codegen.extensions.AnnotationSpec
import com.rsicarelli.fakt.codegen.extensions.MethodSpec as RenderableMethodSpec
import com.rsicarelli.fakt.codegen.extensions.PropertySpec as RenderablePropertySpec
import com.rsicarelli.fakt.codegen.renderer.render
import com.rsicarelli.fakt.codegen.strategy.DefaultValueResolver

/*
 * Maps the pure FakeDeclaration.* contract onto the renderer-facing MethodSpec/PropertySpec from
 * codegen.extensions. The renderer types share the same simple names as the analysis records, so
 * they're aliased here as Renderable*Spec to keep this file readable.
 */

private val defaultValueResolver = DefaultValueResolver()

internal fun FunctionSpec.toRenderableMethodSpec(): RenderableMethodSpec {
    val paramTriples =
        parameters.map { param -> Triple(param.name, param.typeString, param.isVararg) }

    val isVararg = parameters.any { it.isVararg }

    val formattedTypeParams =
        typeParameters.map { typeParam ->
            val bound = typeParameterBounds[typeParam]
            if (bound != null) "$typeParam : $bound" else typeParam
        }

    val defaultBehavior =
        computeDefaultBehavior(returnTypeString, extensionReceiverTypeString, paramTriples)

    return RenderableMethodSpec(
        name = name,
        params = paramTriples,
        returnType = returnTypeString,
        isSuspend = isSuspend,
        isVararg = isVararg,
        typeParameters = formattedTypeParams,
        isOperator = isOperator,
        extensionReceiverType = extensionReceiverTypeString,
        defaultBehavior = defaultBehavior,
    )
}

internal fun PropertySpec.toRenderablePropertySpec(): RenderablePropertySpec {
    val isStateFlow = typeString.contains("StateFlow<")
    val parsedType = parseType(typeString)
    val defaultExpr = defaultValueResolver.resolve(parsedType).render()
    val defaultBehavior = "{ $defaultExpr }"

    return RenderablePropertySpec(
        name = name,
        type = typeString,
        isStateFlow = isStateFlow,
        isMutable = isMutable,
        defaultBehavior = defaultBehavior,
    )
}

internal fun DeclarationAnnotation.toAnnotationSpec(): AnnotationSpec =
    AnnotationSpec(
        simpleName = simpleName,
        fullyQualifiedName = fullyQualifiedName,
        arguments = renderedArguments,
        isOptInMarker = isOptInMarker,
    )

/** @return Pair of (methods, properties) ready for `generateCompleteFake()`. */
internal fun FakeDeclaration.Interface.toCodegenSpecs():
    Pair<List<RenderableMethodSpec>, List<RenderablePropertySpec>> {
    val methodSpecs = functions.map { it.toRenderableMethodSpec() }
    val propertySpecs = properties.map { it.toRenderablePropertySpec() }
    return methodSpecs to propertySpecs
}

/**
 * Maps class members onto renderer specs, preserving the abstract / open distinction via
 * `RenderableMethodSpec.isAbstract` / `RenderablePropertySpec.isAbstract`.
 */
internal fun FakeDeclaration.Class.toCodegenSpecs():
    Pair<List<RenderableMethodSpec>, List<RenderablePropertySpec>> {
    val methodSpecs =
        abstractMethods.map { it.toRenderableMethodSpec().copy(isAbstract = true) } +
            openMethods.map { it.toRenderableMethodSpec().copy(isAbstract = false) }
    val propertySpecs =
        abstractProperties.map { it.toRenderablePropertySpec().copy(isAbstract = true) } +
            openProperties.map { it.toRenderablePropertySpec().copy(isAbstract = false) }
    return methodSpecs to propertySpecs
}

private fun computeDefaultBehavior(
    returnTypeString: String,
    extensionReceiverTypeString: String?,
    paramTriples: List<Triple<String, String, Boolean>>,
): String {
    val parsedReturnType = parseType(returnTypeString)
    val defaultExpr = defaultValueResolver.resolve(parsedReturnType).render()

    val baseParamNames = paramTriples.mapIndexed { i, _ -> "p$i" }
    val lambdaParamNames =
        if (extensionReceiverTypeString != null) listOf("p_receiver") + baseParamNames
        else baseParamNames
    val lambdaParams =
        if (lambdaParamNames.isEmpty()) "" else "${lambdaParamNames.joinToString(", ")} -> "
    return "{ $lambdaParams$defaultExpr }"
}
