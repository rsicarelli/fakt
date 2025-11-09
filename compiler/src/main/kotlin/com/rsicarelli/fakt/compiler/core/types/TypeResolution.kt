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
    fun irTypeToKotlinString(
        irType: IrType,
        preserveTypeParameters: Boolean,
    ): String

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
}

/**
 * Default implementation of TypeResolution that coordinates specialized handlers.
 */
internal class TypeResolutionImpl(
    private val typeRenderer: TypeRenderer,
    private val defaultValueProvider: DefaultValueProvider,
) : TypeResolution {
    override fun irTypeToKotlinString(
        irType: IrType,
        preserveTypeParameters: Boolean,
    ): String = typeRenderer.render(irType, preserveTypeParameters)

    override fun getDefaultValue(irType: IrType): String = defaultValueProvider.provide(irType)

    override fun isPrimitiveType(irType: IrType): Boolean = typeRenderer.isPrimitive(irType)
}

/**
 * Factory function to create a TypeResolution instance with all required handlers.
 */
internal fun createTypeResolution(): TypeResolution {
    val functionTypeHandler = FunctionTypeHandler()
    val genericTypeHandler = GenericTypeHandler()
    val typeRenderer = TypeRenderer(genericTypeHandler, functionTypeHandler)
    val defaultValueProvider = DefaultValueProvider(functionTypeHandler)
    return TypeResolutionImpl(typeRenderer, defaultValueProvider)
}
