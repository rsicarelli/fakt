// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.irnative.types

import dev.rsicarelli.ktfake.compiler.irnative.analysis.TypeAnalysis
import org.jetbrains.kotlin.ir.types.IrType

/**
 * Type mapping and default value generation for fake implementations.
 *
 * This module handles:
 * - Mapping Kotlin types to appropriate default values
 * - Generic type handling with bounds and variance
 * - Custom type support through extension mechanism
 * - Nullable type handling
 * - Complex types (Result, Flow, suspend functions)
 *
 * Key principle: Comprehensive type support with extensible default generation.
 */
interface TypeMapper {

    /**
     * Map a Kotlin type to its default value for fake implementations.
     *
     * @param type Type to map
     * @return Default value expression for the type
     */
    fun mapTypeToDefault(type: TypeAnalysis): DefaultValueExpression

    /**
     * Generate appropriate return type for fake method implementations.
     *
     * @param returnType Method return type
     * @return Expression that can be used as method return
     */
    fun generateReturnExpression(returnType: TypeAnalysis): String

    /**
     * Handle generic type parameters with proper bounds.
     *
     * @param genericType Generic type information
     * @return Generic handling strategy
     */
    fun handleGenericType(genericType: TypeAnalysis): GenericHandling

    /**
     * Register custom type mappings for specific types.
     *
     * @param typeName Fully qualified type name
     * @param defaultGenerator Generator function for default values
     */
    fun registerCustomTypeMapping(typeName: String, defaultGenerator: TypeDefaultGenerator)
}

/**
 * Default value expression that can be rendered in generated code.
 */
sealed class DefaultValueExpression {
    data class Literal(val value: String) : DefaultValueExpression()
    data class Constructor(val className: String, val args: List<String>) : DefaultValueExpression()
    data class FactoryCall(val factoryMethod: String, val args: List<String>) : DefaultValueExpression()
    data class Lambda(val parameters: List<String>, val body: String) : DefaultValueExpression()
    object Null : DefaultValueExpression()
    object EmptyCollection : DefaultValueExpression()
    data class Custom(val expression: String) : DefaultValueExpression()
}

/**
 * Strategy for handling generic types in fake implementations.
 */
sealed class GenericHandling {
    object UseUpperBound : GenericHandling()
    object UseTypeParameter : GenericHandling()
    data class UseSpecificType(val type: String) : GenericHandling()
    data class UseMock(val mockStrategy: MockStrategy) : GenericHandling()
}

/**
 * Mock strategies for complex generic types.
 */
enum class MockStrategy {
    EMPTY_IMPLEMENTATION,
    DELEGATE_TO_FAKE,
    THROW_NOT_IMPLEMENTED,
    RETURN_DEFAULT_VALUE
}

/**
 * Generator for custom type defaults.
 */
fun interface TypeDefaultGenerator {
    fun generateDefault(type: TypeAnalysis): DefaultValueExpression
}

/**
 * Built-in type mappings for common Kotlin types.
 */
object KotlinBuiltinTypes {
    val DEFAULT_MAPPINGS = mapOf(
        "kotlin.String" to DefaultValueExpression.Literal("\"\""),
        "kotlin.Int" to DefaultValueExpression.Literal("0"),
        "kotlin.Long" to DefaultValueExpression.Literal("0L"),
        "kotlin.Float" to DefaultValueExpression.Literal("0.0f"),
        "kotlin.Double" to DefaultValueExpression.Literal("0.0"),
        "kotlin.Boolean" to DefaultValueExpression.Literal("false"),
        "kotlin.Unit" to DefaultValueExpression.Literal("Unit"),

        // Collections
        "kotlin.collections.List" to DefaultValueExpression.FactoryCall("emptyList", emptyList()),
        "kotlin.collections.Set" to DefaultValueExpression.FactoryCall("emptySet", emptyList()),
        "kotlin.collections.Map" to DefaultValueExpression.FactoryCall("emptyMap", emptyList()),

        // Nullable types
        "kotlin.String?" to DefaultValueExpression.Null,
        "kotlin.Int?" to DefaultValueExpression.Null,

        // Result type
        "kotlin.Result" to DefaultValueExpression.FactoryCall("Result.success", listOf("Unit")),

        // Coroutines
        "kotlinx.coroutines.flow.Flow" to DefaultValueExpression.FactoryCall("emptyFlow", emptyList()),
        "kotlinx.coroutines.Deferred" to DefaultValueExpression.Custom("TODO(\"Deferred not supported yet\")")
    )
}
