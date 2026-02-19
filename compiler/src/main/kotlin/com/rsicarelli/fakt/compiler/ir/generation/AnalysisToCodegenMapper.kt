// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.codegen.builder.parseType
import com.rsicarelli.fakt.codegen.extensions.MethodSpec
import com.rsicarelli.fakt.codegen.extensions.PropertySpec
import com.rsicarelli.fakt.codegen.renderer.render
import com.rsicarelli.fakt.codegen.strategy.DefaultValueResolver
import com.rsicarelli.fakt.compiler.core.types.TypeResolution
import com.rsicarelli.fakt.compiler.ir.analysis.FunctionAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.PropertyAnalysis

/*
 * Maps IR analysis models to codegen DSL models.
 *
 * Bridges the gap between IR/FIR compiler analysis and type-safe code generation.
 * Includes default behavior computation so both FakeImpl and factory can use them.
 */

/** Shared resolver for computing default behavior expressions. */
private val defaultValueResolver = DefaultValueResolver()

/**
 * Converts FunctionAnalysis to MethodSpec for DSL-based code generation.
 *
 * @param typeResolver Used to render IrType to String representation
 */
internal fun FunctionAnalysis.toMethodSpec(typeResolver: TypeResolution): MethodSpec {
    // Map parameters to (name, type, isVararg) triples
    val paramTriples =
        parameters.map { param ->
            val typeStr =
                typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
            Triple(param.name, typeStr, param.isVararg)
        }

    val returnTypeStr = typeResolver.irTypeToKotlinString(returnType, preserveTypeParameters = true)

    // Check if this is a vararg function
    val isVararg = parameters.any { it.isVararg }

    // Format method-level type parameters with bounds
    // E.g., ["T"] or ["R : TValue"] or ["T", "R : Comparable<R>"]
    val formattedTypeParams =
        typeParameters.map { typeParam ->
            // If bounds exist in the map, include them
            val bound = typeParameterBounds[typeParam]
            if (bound != null) "$typeParam : $bound" else typeParam
        }

    // Extract extension receiver type if present
    val extensionReceiverTypeStr =
        extensionReceiverType?.let {
            typeResolver.irTypeToKotlinString(it, preserveTypeParameters = true)
        }

    // Compute default behavior expression for the return type
    val parsedReturnType = parseType(returnTypeStr)
    val defaultExpr = defaultValueResolver.resolve(parsedReturnType).render()

    // Build the default lambda with parameter placeholders
    val baseParamNames = paramTriples.mapIndexed { i, _ -> "p$i" }
    val lambdaParamNames =
        if (extensionReceiverTypeStr != null) {
            listOf("p_receiver") + baseParamNames
        } else {
            baseParamNames
        }
    val lambdaParams =
        if (lambdaParamNames.isEmpty()) "" else "${lambdaParamNames.joinToString(", ")} -> "
    val defaultBehavior = "{ $lambdaParams$defaultExpr }"

    return MethodSpec(
        name = name,
        params = paramTriples,
        returnType = returnTypeStr,
        isSuspend = isSuspend,
        isVararg = isVararg,
        typeParameters = formattedTypeParams,
        isOperator = isOperator,
        extensionReceiverType = extensionReceiverTypeStr,
        defaultBehavior = defaultBehavior,
    )
}

/**
 * Converts PropertyAnalysis to PropertySpec for DSL-based code generation.
 *
 * @param typeResolver Used to render IrType to String representation
 */
internal fun PropertyAnalysis.toPropertySpec(typeResolver: TypeResolution): PropertySpec {
    val typeStr = typeResolver.irTypeToKotlinString(type, preserveTypeParameters = true)

    // Detect if this is a StateFlow property by checking the type name
    val isStateFlow = typeStr.contains("StateFlow<")

    // Compute default behavior expression for the property type
    val parsedType = parseType(typeStr)
    val defaultExpr = defaultValueResolver.resolve(parsedType).render()
    val defaultBehavior = "{ $defaultExpr }"

    return PropertySpec(
        name = name,
        type = typeStr,
        isStateFlow = isStateFlow,
        isMutable = isMutable,
        defaultBehavior = defaultBehavior,
    )
}

/**
 * Converts all functions and properties from InterfaceAnalysis to codegen specs.
 *
 * @return Pair of (methods, properties) ready for generateCompleteFake()
 */
internal fun InterfaceAnalysis.toCodegenSpecs(
    typeResolver: TypeResolution
): Pair<List<MethodSpec>, List<PropertySpec>> {
    val methodSpecs = functions.map { it.toMethodSpec(typeResolver) }
    val propertySpecs = properties.map { it.toPropertySpec(typeResolver) }

    return methodSpecs to propertySpecs
}
