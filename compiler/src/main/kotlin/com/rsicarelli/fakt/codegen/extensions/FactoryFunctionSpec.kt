// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Configuration for generating a factory function.
 *
 * @property interfaceName The name of the interface being faked
 * @property typeParameters List of type parameters with constraints
 * @property visibility Visibility for the generated function
 * @property annotations Annotations to propagate to the factory function
 * @property methods Optional list of methods for KDoc generation
 * @property properties Optional list of properties for KDoc generation
 */
data class FactoryFunctionSpec(
    val interfaceName: String,
    val superTypeName: String = interfaceName,
    val typeParameters: List<String> = emptyList(),
    val visibility: FirVisibility = FirVisibility.PUBLIC,
    val annotations: List<AnnotationSpec> = emptyList(),
    val methods: List<MethodSpec> = emptyList(),
    val properties: List<PropertySpec> = emptyList(),
)
