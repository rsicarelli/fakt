// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.CodeFileBuilder
import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.model.CodeDeclaration
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Adds all call history components to a [CodeFileBuilder] using the DSL.
 *
 * For ALL methods:
 * - Methods with params: data class + full verifier + verify function
 * - 0-param/vararg-only: simplified verifier (wasCalledTimes/wasNeverCalled) + verify function
 *
 * @param fakeClassName Name of the fake implementation class
 * @param interfaceName Name of the interface (for collision-safe data class naming)
 * @param methods List of method specs to generate history for
 * @param visibility Visibility modifier for generated code
 * @param classTypeParameters Class-level type parameters that need to be erased to Any?
 */
internal fun CodeFileBuilder.addCallHistoryComponents(
    fakeClassName: String,
    interfaceName: String,
    methods: List<MethodSpec>,
    visibility: FirVisibility,
    classTypeParameters: List<String> = emptyList(),
) {
    if (methods.isEmpty()) return

    val methodsWithParams = methods.filter { it.params.any { p -> !p.third } }
    val zeroParamMethods = methods.filter { it.params.none { p -> !p.third } }
    val classTypeParamNames = classTypeParameters.map(::extractTypeParamName).toSet()

    // Data classes for methods with params
    methodsWithParams.forEach { method ->
        val typeParams =
            classTypeParamNames + method.typeParameters.map(::extractTypeParamName).toSet()
        callDataClass(interfaceName, method.name, method.params, visibility, typeParams)
    }

    // Verifier classes for all methods
    methodsWithParams.forEach { method ->
        val typeParams =
            classTypeParamNames + method.typeParameters.map(::extractTypeParamName).toSet()
        verifierClass(interfaceName, method.name, method.params, visibility, typeParams)
    }
    zeroParamMethods.forEach { method -> unitVerifierClass(interfaceName, method.name, visibility) }

    // Verify functions for all methods
    methods.forEach { method ->
        val hasParams = method.params.any { !it.third }
        if (hasParams) {
            verifyFunction(
                fakeClassName,
                interfaceName,
                method.name,
                visibility,
                classTypeParameters,
            )
        } else {
            unitVerifyFunction(
                fakeClassName,
                interfaceName,
                method.name,
                visibility,
                classTypeParameters,
            )
        }
    }
}

/**
 * Generates call history declarations as a list that can be added to an existing CodeFile.
 *
 * This function creates a temporary CodeFileBuilder, adds all call history components, and returns
 * the declarations.
 *
 * @param fakeClassName Name of the fake implementation class
 * @param interfaceName Name of the interface (for collision-safe data class naming)
 * @param methods List of method specs to generate history for
 * @param visibility Visibility modifier for generated code
 * @param classTypeParameters Class-level type parameters that need to be erased to Any?
 * @return List of declarations for call history (data classes, verifier classes, verify functions)
 */
internal fun generateCallHistoryDeclarations(
    fakeClassName: String,
    interfaceName: String,
    methods: List<MethodSpec>,
    visibility: FirVisibility,
    classTypeParameters: List<String> = emptyList(),
): List<CodeDeclaration> {
    if (methods.isEmpty()) return emptyList()

    val tempFile =
        codeFile("") {
            addCallHistoryComponents(
                fakeClassName,
                interfaceName,
                methods,
                visibility,
                classTypeParameters,
            )
        }

    return tempFile.declarations
}
