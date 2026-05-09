// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.core.types

import org.jetbrains.kotlin.ir.types.IrType

/**
 * Facade for type resolution operations.
 *
 * Provides a clean interface for:
 * - Converting IR types to Kotlin string representations
 * - Generating appropriate default values for types
 * - Detecting primitive types
 * - Emitting [RenderedType] side-channel data (FQN collection for import resolution, 3.1.d.1)
 * - Computing semantic flags hoisted from the renderer (3.1.d.1)
 *
 * This facade delegates to specialized handlers for different type categories.
 */
internal interface TypeResolution {
    /**
     * Converts IR type to readable Kotlin string representation.
     *
     * @param irType The IR type to convert
     * @param preserveTypeParameters Whether to preserve generic type parameters
     * @return String representation of the type
     */
    fun irTypeToKotlinString(irType: IrType, preserveTypeParameters: Boolean): String

    /**
     * Renders [irType] and returns a [RenderedType] bundling the short name with all
     * fully-qualified names referenced by that string.
     *
     * Introduced in 3.1.d.1 as the side-channel that will eventually replace IR traversal in
     * [com.rsicarelli.fakt.compiler.core.context.ImportResolver].
     *
     * @param irType The IR type to render
     * @param preserveTypeParameters Whether to preserve generic type parameter names
     * @return [RenderedType] with short name and FQN set
     */
    fun irTypeToRendered(irType: IrType, preserveTypeParameters: Boolean): RenderedType

    /**
     * Generates appropriate default values for IR types.
     *
     * @param irType The type to generate a default value for
     * @return String representation of the default value
     */
    fun getDefaultValue(irType: IrType): String

    /**
     * Check if a type is primitive and doesn't need imports.
     *
     * @param irType The type to check
     * @return true if the type is primitive, false otherwise
     */
    fun isPrimitiveType(irType: IrType): Boolean

    /**
     * Returns `true` when [irType] is a type-parameter placeholder (T, K, V …).
     *
     * Semantic flag hoisted from [TypeRenderer] in 3.1.d.1 via [IrTypeSemantics].
     */
    fun isTypeParameter(irType: IrType): Boolean

    /**
     * Returns `true` when [irType] is a collection whose type arguments must be erased to `Any`
     * under the NoGenerics pattern.
     *
     * Semantic flag hoisted from [GenericTypeHandler] in 3.1.d.1 via [IrTypeSemantics].
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
