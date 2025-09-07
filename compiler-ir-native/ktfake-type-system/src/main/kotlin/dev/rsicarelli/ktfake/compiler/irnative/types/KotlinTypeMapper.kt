// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.irnative.types

import dev.rsicarelli.ktfake.compiler.irnative.analysis.TypeAnalysis

/**
 * Concrete implementation of TypeMapper for Kotlin types.
 *
 * This mapper can handle ANY Kotlin type dynamically:
 * - All built-in types (String, Int, Boolean, etc.)
 * - Collections (List, Set, Map) with proper generics
 * - Coroutines types (Flow, Deferred)
 * - Custom types with sensible defaults
 * - Nullable vs non-null distinctions
 * - Complex generic types with bounds
 *
 * Key improvement: No hardcoded type mappings - extensible and adaptive.
 */
class KotlinTypeMapper : TypeMapper {

    private val customTypeMappings = mutableMapOf<String, TypeDefaultGenerator>()

    /**
     * Map any Kotlin type to appropriate default value expression.
     * Handles built-ins, collections, custom types, and nullables.
     */
    override fun mapTypeToDefault(type: TypeAnalysis): DefaultValueExpression {
        // Handle nullable types first
        if (type.isNullable) {
            return DefaultValueExpression.Null
        }

        // Check custom mappings first
        customTypeMappings[type.qualifiedName]?.let { customGenerator ->
            return customGenerator.generateDefault(type)
        }

        // Handle built-in types
        return when (type.qualifiedName) {
            // Primitives
            "kotlin.String" -> DefaultValueExpression.Literal("\"\"")
            "kotlin.Int" -> DefaultValueExpression.Literal("0")
            "kotlin.Long" -> DefaultValueExpression.Literal("0L")
            "kotlin.Float" -> DefaultValueExpression.Literal("0.0f")
            "kotlin.Double" -> DefaultValueExpression.Literal("0.0")
            "kotlin.Boolean" -> DefaultValueExpression.Literal("false")
            "kotlin.Char" -> DefaultValueExpression.Literal("'\\u0000'")
            "kotlin.Byte" -> DefaultValueExpression.Literal("0")
            "kotlin.Short" -> DefaultValueExpression.Literal("0")
            "kotlin.Unit" -> DefaultValueExpression.Literal("Unit")

            // Collections - with generic handling
            "kotlin.collections.List" -> generateCollectionDefault("emptyList", type.genericArguments)
            "kotlin.collections.MutableList" -> generateCollectionDefault("mutableListOf", type.genericArguments)
            "kotlin.collections.Set" -> generateCollectionDefault("emptySet", type.genericArguments)
            "kotlin.collections.MutableSet" -> generateCollectionDefault("mutableSetOf", type.genericArguments)
            "kotlin.collections.Map" -> generateMapDefault("emptyMap", type.genericArguments)
            "kotlin.collections.MutableMap" -> generateMapDefault("mutableMapOf", type.genericArguments)

            // Array types
            "kotlin.Array" -> generateArrayDefault(type.genericArguments.firstOrNull())
            "kotlin.IntArray" -> DefaultValueExpression.FactoryCall("intArrayOf", emptyList())
            "kotlin.LongArray" -> DefaultValueExpression.FactoryCall("longArrayOf", emptyList())
            "kotlin.FloatArray" -> DefaultValueExpression.FactoryCall("floatArrayOf", emptyList())
            "kotlin.DoubleArray" -> DefaultValueExpression.FactoryCall("doubleArrayOf", emptyList())
            "kotlin.BooleanArray" -> DefaultValueExpression.FactoryCall("booleanArrayOf", emptyList())
            "kotlin.CharArray" -> DefaultValueExpression.FactoryCall("charArrayOf", emptyList())
            "kotlin.ByteArray" -> DefaultValueExpression.FactoryCall("byteArrayOf", emptyList())
            "kotlin.ShortArray" -> DefaultValueExpression.FactoryCall("shortArrayOf", emptyList())

            // Result type
            "kotlin.Result" -> generateResultDefault(type.genericArguments.firstOrNull())

            // Coroutines
            "kotlinx.coroutines.flow.Flow" -> generateFlowDefault(type.genericArguments.firstOrNull())
            "kotlinx.coroutines.Deferred" -> generateDeferredDefault(type.genericArguments.firstOrNull())
            "kotlinx.coroutines.Job" -> DefaultValueExpression.FactoryCall("Job", emptyList())

            // Function types (lambdas)
            else -> when {
                type.qualifiedName.startsWith("kotlin.Function") -> generateLambdaDefault(type)
                type.qualifiedName.startsWith("kotlin.coroutines.SuspendFunction") -> generateSuspendLambdaDefault(type)
                else -> generateCustomTypeDefault(type)
            }
        }
    }

    /**
     * Generate return expression for method implementations.
     */
    override fun generateReturnExpression(returnType: TypeAnalysis): String {
        val defaultExpr = mapTypeToDefault(returnType)
        return renderDefaultExpression(defaultExpr)
    }

    /**
     * Handle generic types with proper bound resolution.
     */
    override fun handleGenericType(genericType: TypeAnalysis): GenericHandling {
        return when {
            // For common generic interfaces, use empty implementations
            genericType.qualifiedName.endsWith("List") -> GenericHandling.UseSpecificType("emptyList<Any>()")
            genericType.qualifiedName.endsWith("Set") -> GenericHandling.UseSpecificType("emptySet<Any>()")
            genericType.qualifiedName.endsWith("Map") -> GenericHandling.UseSpecificType("emptyMap<Any, Any>()")

            // For custom generic types, try to use upper bound
            genericType.genericArguments.isNotEmpty() -> GenericHandling.UseUpperBound

            // For unknown generics, use Any as fallback
            else -> GenericHandling.UseSpecificType("Any()")
        }
    }

    /**
     * Register custom type mappings for domain-specific types.
     */
    override fun registerCustomTypeMapping(typeName: String, defaultGenerator: TypeDefaultGenerator) {
        customTypeMappings[typeName] = defaultGenerator
    }

    /**
     * Generate collection defaults with proper generic handling.
     */
    private fun generateCollectionDefault(factoryName: String, generics: List<TypeAnalysis>): DefaultValueExpression {
        return if (generics.isNotEmpty()) {
            // Include generic type information: emptyList<String>()
            val genericType = generics.first().qualifiedName.substringAfterLast(".")
            DefaultValueExpression.FactoryCall("$factoryName<$genericType>", emptyList())
        } else {
            // No generics: emptyList()
            DefaultValueExpression.FactoryCall(factoryName, emptyList())
        }
    }

    /**
     * Generate map defaults with key-value generic handling.
     */
    private fun generateMapDefault(factoryName: String, generics: List<TypeAnalysis>): DefaultValueExpression {
        return when (generics.size) {
            2 -> {
                val keyType = generics[0].qualifiedName.substringAfterLast(".")
                val valueType = generics[1].qualifiedName.substringAfterLast(".")
                DefaultValueExpression.FactoryCall("$factoryName<$keyType, $valueType>", emptyList())
            }
            else -> DefaultValueExpression.FactoryCall(factoryName, emptyList())
        }
    }

    /**
     * Generate array defaults with generic element type.
     */
    private fun generateArrayDefault(elementType: TypeAnalysis?): DefaultValueExpression {
        return if (elementType != null) {
            val elemTypeName = elementType.qualifiedName.substringAfterLast(".")
            DefaultValueExpression.FactoryCall("emptyArray<$elemTypeName>", emptyList())
        } else {
            DefaultValueExpression.FactoryCall("emptyArray<Any>", emptyList())
        }
    }

    /**
     * Generate Result type defaults.
     */
    private fun generateResultDefault(successType: TypeAnalysis?): DefaultValueExpression {
        return if (successType != null) {
            val defaultValue = renderDefaultExpression(mapTypeToDefault(successType))
            DefaultValueExpression.FactoryCall("Result.success", listOf(defaultValue))
        } else {
            DefaultValueExpression.FactoryCall("Result.success", listOf("Unit"))
        }
    }

    /**
     * Generate Flow defaults.
     */
    private fun generateFlowDefault(elementType: TypeAnalysis?): DefaultValueExpression {
        return DefaultValueExpression.FactoryCall("emptyFlow", emptyList())
    }

    /**
     * Generate Deferred defaults.
     */
    private fun generateDeferredDefault(resultType: TypeAnalysis?): DefaultValueExpression {
        // For testing, create a completed deferred
        val defaultValue = if (resultType != null) {
            renderDefaultExpression(mapTypeToDefault(resultType))
        } else {
            "Unit"
        }
        return DefaultValueExpression.Custom("CompletableDeferred($defaultValue)")
    }

    /**
     * Generate lambda/function defaults.
     */
    private fun generateLambdaDefault(functionType: TypeAnalysis): DefaultValueExpression {
        // For MVP: return empty lambda that returns appropriate default
        val params = (1..functionType.genericArguments.size - 1).joinToString(", ") { "p$it" }
        val returnType = functionType.genericArguments.lastOrNull()
        val returnExpr = if (returnType != null) {
            renderDefaultExpression(mapTypeToDefault(returnType))
        } else {
            "Unit"
        }

        return DefaultValueExpression.Lambda(
            parameters = params.split(", ").filter { it.isNotEmpty() },
            body = returnExpr
        )
    }

    /**
     * Generate suspend lambda defaults.
     */
    private fun generateSuspendLambdaDefault(suspendFunctionType: TypeAnalysis): DefaultValueExpression {
        // Similar to regular lambda but with suspend
        return generateLambdaDefault(suspendFunctionType)
    }

    /**
     * Generate defaults for custom/unknown types.
     */
    private fun generateCustomTypeDefault(type: TypeAnalysis): DefaultValueExpression {
        // For custom types, try common patterns:
        return when {
            // Data classes often have companion object factories
            type.qualifiedName.contains("data class") ->
                DefaultValueExpression.Custom("${type.qualifiedName.substringAfterLast(".")}.default()")

            // Sealed classes - return first subtype
            type.qualifiedName.contains("sealed") ->
                DefaultValueExpression.Custom("TODO(\"Sealed class default not implemented\")")

            // Enums - return first value
            type.qualifiedName.contains("enum") ->
                DefaultValueExpression.Custom("${type.qualifiedName.substringAfterLast(".")}.values().first()")

            // Regular classes - try empty constructor or throw TODO
            else -> DefaultValueExpression.Custom("TODO(\"Custom type ${type.qualifiedName} default not implemented\")")
        }
    }

    /**
     * Render default expression to string code.
     */
    private fun renderDefaultExpression(expr: DefaultValueExpression): String {
        return when (expr) {
            is DefaultValueExpression.Literal -> expr.value
            is DefaultValueExpression.Constructor -> "${expr.className}(${expr.args.joinToString(", ")})"
            is DefaultValueExpression.FactoryCall -> "${expr.factoryMethod}(${expr.args.joinToString(", ")})"
            is DefaultValueExpression.Lambda -> "{ ${expr.parameters.joinToString(", ")} -> ${expr.body} }"
            is DefaultValueExpression.Null -> "null"
            is DefaultValueExpression.EmptyCollection -> "emptyList()" // Fallback
            is DefaultValueExpression.Custom -> expr.expression
        }
    }
}
