// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.ir.generation

import com.rsicarelli.fakt.codegen.extensions.MethodSpec
import com.rsicarelli.fakt.codegen.extensions.PropertySpec
import com.rsicarelli.fakt.compiler.ir.analysis.FunctionAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.InterfaceAnalysis
import com.rsicarelli.fakt.compiler.ir.analysis.PropertyAnalysis
import com.rsicarelli.fakt.compiler.core.types.TypeResolution

/**
 * Maps IR analysis models to codegen DSL models.
 *
 * Bridges the gap between IR/FIR compiler analysis and type-safe code generation.
 */

/**
 * Converts FunctionAnalysis to MethodSpec for DSL-based code generation.
 *
 * @param typeResolver Used to render IrType to String representation
 */
internal fun FunctionAnalysis.toMethodSpec(typeResolver: TypeResolution): MethodSpec {
    // Map parameters to (name, type, isVararg) triples
    val paramTriples = parameters.map { param ->
        val typeStr = typeResolver.irTypeToKotlinString(param.type, preserveTypeParameters = true)
        Triple(param.name, typeStr, param.isVararg)
    }

    val returnTypeStr = typeResolver.irTypeToKotlinString(returnType, preserveTypeParameters = true)

    // Check if this is a vararg function
    val isVararg = parameters.any { it.isVararg }

    // Format method-level type parameters with bounds
    // E.g., ["T"] or ["R : TValue"] or ["T", "R : Comparable<R>"]
    val formattedTypeParams = typeParameters.map { typeParam ->
        // If bounds exist in the map, include them
        val bound = typeParameterBounds[typeParam]
        if (bound != null) "$typeParam : $bound" else typeParam
    }

    return MethodSpec(
        name = name,
        params = paramTriples,
        returnType = returnTypeStr,
        isSuspend = isSuspend,
        isVararg = isVararg,
        typeParameters = formattedTypeParams
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

    return PropertySpec(
        name = name,
        type = typeStr,
        isStateFlow = isStateFlow,
        isMutable = isMutable
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
