// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.ClassBuilder
import com.rsicarelli.fakt.codegen.builder.DataClassBuilder
import com.rsicarelli.fakt.codegen.builder.FunctionBuilder
import com.rsicarelli.fakt.codegen.builder.PropertyBuilder
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility

/**
 * Applies visibility modifier to a [ClassBuilder].
 */
internal fun FirVisibility.applyVisibility(builder: ClassBuilder) {
    when (this) {
        FirVisibility.PUBLIC -> builder.public()
        FirVisibility.INTERNAL -> builder.internal()
        FirVisibility.PRIVATE, FirVisibility.PROTECTED -> {} // Not applicable for top-level classes
    }
}

/**
 * Applies visibility modifier to a [DataClassBuilder].
 */
internal fun FirVisibility.applyVisibility(builder: DataClassBuilder) {
    when (this) {
        FirVisibility.PUBLIC -> builder.public()
        FirVisibility.INTERNAL -> builder.internal()
        FirVisibility.PRIVATE, FirVisibility.PROTECTED -> {} // Not applicable for data classes
    }
}

/**
 * Applies visibility modifier to a [FunctionBuilder].
 */
internal fun FirVisibility.applyVisibility(builder: FunctionBuilder) {
    when (this) {
        FirVisibility.PUBLIC -> builder.public()
        FirVisibility.INTERNAL -> builder.internal()
        FirVisibility.PRIVATE, FirVisibility.PROTECTED -> {} // Handled elsewhere
    }
}

/**
 * Applies visibility modifier to a [PropertyBuilder].
 */
internal fun FirVisibility.applyVisibility(builder: PropertyBuilder) {
    when (this) {
        FirVisibility.PUBLIC -> builder.public()
        FirVisibility.INTERNAL -> builder.internal()
        FirVisibility.PRIVATE -> builder.private()
        FirVisibility.PROTECTED -> {} // Handled elsewhere
    }
}
