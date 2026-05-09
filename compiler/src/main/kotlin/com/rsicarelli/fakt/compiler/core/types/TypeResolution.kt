// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.types

import org.jetbrains.kotlin.ir.types.IrType

/**
 * Facade for IR-type resolution operations: rendering to Kotlin strings, default-value lookup,
 * primitive detection, and the [RenderedType] FQN side-channel that
 * [com.rsicarelli.fakt.compiler.core.context.ImportResolver] consumes.
 *
 * Delegates to specialized handlers for each type category.
 */
internal interface TypeResolution {
    /** Renders [irType] as a Kotlin source-form string (e.g. `"List<User>?"`). */
    fun irTypeToKotlinString(irType: IrType, preserveTypeParameters: Boolean): String

    /**
     * Renders [irType] and returns the short name plus the set of FQNs referenced by that string.
     */
    fun irTypeToRendered(irType: IrType, preserveTypeParameters: Boolean): RenderedType

    /** Returns a Kotlin source expression for the default value of [irType]. */
    fun getDefaultValue(irType: IrType): String

    /** True when [irType] is a primitive that requires no import. */
    fun isPrimitiveType(irType: IrType): Boolean

    /** True when [irType] is a type-parameter placeholder (T, K, V …). */
    fun isTypeParameter(irType: IrType): Boolean

    /**
     * True when [irType] is a stdlib collection whose type arguments must be erased to `Any` under
     * the NoGenerics pattern.
     */
    fun requiresCollectionErasure(irType: IrType): Boolean
}

/** Default implementation of TypeResolution that coordinates specialized handlers. */
internal class TypeResolutionImpl(
    private val typeRenderer: TypeRenderer,
    private val defaultValueProvider: DefaultValueProvider,
    private val typeSemantics: IrTypeSemantics,
) : TypeResolution {
    override fun irTypeToKotlinString(irType: IrType, preserveTypeParameters: Boolean): String =
        typeRenderer.render(irType, preserveTypeParameters)

    override fun irTypeToRendered(irType: IrType, preserveTypeParameters: Boolean): RenderedType =
        typeSemantics.buildRenderedType(irType, preserveTypeParameters)

    override fun getDefaultValue(irType: IrType): String = defaultValueProvider.provide(irType)

    override fun isPrimitiveType(irType: IrType): Boolean = typeRenderer.isPrimitive(irType)

    override fun isTypeParameter(irType: IrType): Boolean = typeSemantics.isTypeParameter(irType)

    override fun requiresCollectionErasure(irType: IrType): Boolean =
        typeSemantics.requiresCollectionErasure(irType)
}

/** Factory function to create a TypeResolution instance with all required handlers. */
internal fun createTypeResolution(): TypeResolution {
    val functionTypeHandler = FunctionTypeHandler()
    val genericTypeHandler = GenericTypeHandler()
    val typeRenderer = TypeRenderer(genericTypeHandler, functionTypeHandler)
    val defaultValueProvider = DefaultValueProvider(functionTypeHandler)
    val typeSemantics = IrTypeSemantics(typeRenderer)
    return TypeResolutionImpl(typeRenderer, defaultValueProvider, typeSemantics)
}
