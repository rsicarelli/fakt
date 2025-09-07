// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package dev.rsicarelli.ktfake.compiler.irnative.types

import dev.rsicarelli.ktfake.compiler.irnative.analysis.TypeAnalysis
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Nested
import kotlin.test.*

/**
 * Tests for KotlinTypeMapper following BDD naming conventions and testing guidelines.
 *
 * Tests comprehensive type mapping using vanilla JUnit5:
 * - GIVEN: Various Kotlin types (built-in, collections, custom)
 * - WHEN: Mapping types to default values
 * - THEN: Appropriate default expressions are generated
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KotlinTypeMapperTest {

    private val typeMapper = KotlinTypeMapper()

    @Nested
    inner class BuiltinTypeMappings {

        @Test
        fun `GIVEN String type WHEN mapping to default THEN should return empty string literal`() {
            // Given
            val stringType = createTypeAnalysis("kotlin.String", isNullable = false)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(stringType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.Literal)
            assertEquals("\"\"", (defaultExpr as DefaultValueExpression.Literal).value)
        }

        @Test
        fun `GIVEN Int type WHEN mapping to default THEN should return zero literal`() {
            // Given
            val intType = createTypeAnalysis("kotlin.Int", isNullable = false)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(intType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.Literal)
            assertEquals("0", (defaultExpr as DefaultValueExpression.Literal).value)
        }

        @Test
        fun `GIVEN Boolean type WHEN mapping to default THEN should return false literal`() {
            // Given
            val booleanType = createTypeAnalysis("kotlin.Boolean", isNullable = false)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(booleanType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.Literal)
            assertEquals("false", (defaultExpr as DefaultValueExpression.Literal).value)
        }

        @Test
        fun `GIVEN Unit type WHEN mapping to default THEN should return Unit literal`() {
            // Given
            val unitType = createTypeAnalysis("kotlin.Unit", isNullable = false)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(unitType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.Literal)
            assertEquals("Unit", (defaultExpr as DefaultValueExpression.Literal).value)
        }

        @Test
        fun `GIVEN nullable type WHEN mapping to default THEN should return null`() {
            // Given
            val nullableString = createTypeAnalysis("kotlin.String", isNullable = true)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(nullableString)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.Null)
        }
    }

    @Nested
    inner class CollectionTypeMappings {

        @Test
        fun `GIVEN List type WHEN mapping to default THEN should return empty list factory call`() {
            // Given
            val listType = createTypeAnalysis("kotlin.collections.List", isNullable = false)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(listType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.FactoryCall)
            val factoryCall = defaultExpr as DefaultValueExpression.FactoryCall
            assertEquals("emptyList", factoryCall.factoryMethod)
            assertEquals(emptyList<String>(), factoryCall.args)
        }

        @Test
        fun `GIVEN generic List type WHEN mapping to default THEN should include generic type information`() {
            // Given
            val genericListType = createTypeAnalysis(
                "kotlin.collections.List",
                isNullable = false,
                generics = listOf(createTypeAnalysis("kotlin.String", false))
            )

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(genericListType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.FactoryCall)
            val factoryCall = defaultExpr as DefaultValueExpression.FactoryCall
            assertEquals("emptyList<String>", factoryCall.factoryMethod)
        }

        @Test
        fun `GIVEN Map type WHEN mapping to default THEN should return empty map with key-value generics`() {
            // Given
            val mapType = createTypeAnalysis(
                "kotlin.collections.Map",
                isNullable = false,
                generics = listOf(
                    createTypeAnalysis("kotlin.String", false),
                    createTypeAnalysis("kotlin.Int", false)
                )
            )

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(mapType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.FactoryCall)
            val factoryCall = defaultExpr as DefaultValueExpression.FactoryCall
            assertEquals("emptyMap<String, Int>", factoryCall.factoryMethod)
        }

        @Test
        fun `GIVEN Set type WHEN mapping to default THEN should return empty set factory call`() {
            // Given
            val setType = createTypeAnalysis("kotlin.collections.Set", isNullable = false)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(setType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.FactoryCall)
            val factoryCall = defaultExpr as DefaultValueExpression.FactoryCall
            assertEquals("emptySet", factoryCall.factoryMethod)
        }
    }

    @Nested
    inner class ArrayTypeMappings {

        @Test
        fun `GIVEN Array type WHEN mapping to default THEN should return empty array with generic type`() {
            // Given
            val arrayType = createTypeAnalysis(
                "kotlin.Array",
                isNullable = false,
                generics = listOf(createTypeAnalysis("kotlin.String", false))
            )

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(arrayType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.FactoryCall)
            val factoryCall = defaultExpr as DefaultValueExpression.FactoryCall
            assertEquals("emptyArray<String>", factoryCall.factoryMethod)
        }

        @Test
        fun `GIVEN IntArray type WHEN mapping to default THEN should return int array factory`() {
            // Given
            val intArrayType = createTypeAnalysis("kotlin.IntArray", isNullable = false)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(intArrayType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.FactoryCall)
            val factoryCall = defaultExpr as DefaultValueExpression.FactoryCall
            assertEquals("intArrayOf", factoryCall.factoryMethod)
            assertEquals(emptyList<String>(), factoryCall.args)
        }
    }

    @Nested
    inner class CoroutineTypeMappings {

        @Test
        fun `GIVEN Flow type WHEN mapping to default THEN should return empty flow`() {
            // Given
            val flowType = createTypeAnalysis("kotlinx.coroutines.flow.Flow", isNullable = false)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(flowType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.FactoryCall)
            val factoryCall = defaultExpr as DefaultValueExpression.FactoryCall
            assertEquals("emptyFlow", factoryCall.factoryMethod)
        }

        @Test
        fun `GIVEN Job type WHEN mapping to default THEN should return Job factory`() {
            // Given
            val jobType = createTypeAnalysis("kotlinx.coroutines.Job", isNullable = false)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(jobType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.FactoryCall)
            val factoryCall = defaultExpr as DefaultValueExpression.FactoryCall
            assertEquals("Job", factoryCall.factoryMethod)
        }

        @Test
        fun `GIVEN Deferred type WHEN mapping to default THEN should return completed deferred`() {
            // Given
            val deferredType = createTypeAnalysis(
                "kotlinx.coroutines.Deferred",
                isNullable = false,
                generics = listOf(createTypeAnalysis("kotlin.String", false))
            )

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(deferredType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.Custom)
            val customExpr = defaultExpr as DefaultValueExpression.Custom
            assertEquals("CompletableDeferred(\"\")", customExpr.expression)
        }
    }

    @Nested
    inner class ResultTypeMappings {

        @Test
        fun `GIVEN Result type WHEN mapping to default THEN should return success with default value`() {
            // Given
            val resultType = createTypeAnalysis(
                "kotlin.Result",
                isNullable = false,
                generics = listOf(createTypeAnalysis("kotlin.String", false))
            )

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(resultType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.FactoryCall)
            val factoryCall = defaultExpr as DefaultValueExpression.FactoryCall
            assertEquals("Result.success", factoryCall.factoryMethod)
            assertEquals(listOf("\"\""), factoryCall.args)
        }

        @Test
        fun `GIVEN Result with Unit WHEN mapping to default THEN should return success with Unit`() {
            // Given
            val resultType = createTypeAnalysis(
                "kotlin.Result",
                isNullable = false,
                generics = listOf(createTypeAnalysis("kotlin.Unit", false))
            )

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(resultType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.FactoryCall)
            val factoryCall = defaultExpr as DefaultValueExpression.FactoryCall
            assertEquals("Result.success", factoryCall.factoryMethod)
            assertEquals(listOf("Unit"), factoryCall.args)
        }
    }

    @Nested
    inner class FunctionTypeMappings {

        @Test
        fun `GIVEN function type WHEN mapping to default THEN should return lambda with appropriate return`() {
            // Given - Function1<String, Int>
            val functionType = createTypeAnalysis(
                "kotlin.Function1",
                isNullable = false,
                generics = listOf(
                    createTypeAnalysis("kotlin.String", false),
                    createTypeAnalysis("kotlin.Int", false)
                )
            )

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(functionType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.Lambda)
            val lambda = defaultExpr as DefaultValueExpression.Lambda
            assertEquals(listOf("p1"), lambda.parameters)
            assertEquals("0", lambda.body)
        }

        @Test
        fun `GIVEN function type with Unit return WHEN mapping to default THEN should return lambda with Unit`() {
            // Given - Function1<String, Unit>
            val functionType = createTypeAnalysis(
                "kotlin.Function1",
                isNullable = false,
                generics = listOf(
                    createTypeAnalysis("kotlin.String", false),
                    createTypeAnalysis("kotlin.Unit", false)
                )
            )

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(functionType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.Lambda)
            val lambda = defaultExpr as DefaultValueExpression.Lambda
            assertEquals("Unit", lambda.body)
        }
    }

    @Nested
    inner class CustomTypeMappings {

        @Test
        fun `GIVEN custom type WHEN mapping to default THEN should return TODO for unknown types`() {
            // Given
            val customType = createTypeAnalysis("com.example.CustomService", isNullable = false)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(customType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.Custom)
            val customExpr = defaultExpr as DefaultValueExpression.Custom
            assertTrue(customExpr.expression.contains("TODO"))
            assertTrue(customExpr.expression.contains("CustomService"))
        }

        @Test
        fun `GIVEN custom type mapping registered WHEN mapping to default THEN should use custom generator`() {
            // Given
            val customType = createTypeAnalysis("com.example.User", isNullable = false)
            val customGenerator = TypeDefaultGenerator { _ ->
                DefaultValueExpression.Constructor("User", listOf("\"default\"", "\"user@example.com\""))
            }

            typeMapper.registerCustomTypeMapping("com.example.User", customGenerator)

            // When
            val defaultExpr = typeMapper.mapTypeToDefault(customType)

            // Then
            assertTrue(defaultExpr is DefaultValueExpression.Constructor)
            val constructor = defaultExpr as DefaultValueExpression.Constructor
            assertEquals("User", constructor.className)
            assertEquals(listOf("\"default\"", "\"user@example.com\""), constructor.args)
        }
    }

    @Nested
    inner class GenericTypeHandling {

        @Test
        fun `GIVEN generic List type WHEN handling generics THEN should use specific empty list`() {
            // Given
            val genericListType = createTypeAnalysis("kotlin.collections.List", false)

            // When
            val handling = typeMapper.handleGenericType(genericListType)

            // Then
            assertTrue(handling is GenericHandling.UseSpecificType)
            val specificType = handling as GenericHandling.UseSpecificType
            assertEquals("emptyList<Any>()", specificType.type)
        }

        @Test
        fun `GIVEN generic Map type WHEN handling generics THEN should use specific empty map`() {
            // Given
            val genericMapType = createTypeAnalysis("kotlin.collections.Map", false)

            // When
            val handling = typeMapper.handleGenericType(genericMapType)

            // Then
            assertTrue(handling is GenericHandling.UseSpecificType)
            val specificType = handling as GenericHandling.UseSpecificType
            assertEquals("emptyMap<Any, Any>()", specificType.type)
        }

        @Test
        fun `GIVEN unknown generic type WHEN handling generics THEN should use Any fallback`() {
            // Given
            val unknownGenericType = createTypeAnalysis("com.example.Generic", false)

            // When
            val handling = typeMapper.handleGenericType(unknownGenericType)

            // Then
            assertTrue(handling is GenericHandling.UseSpecificType)
            val specificType = handling as GenericHandling.UseSpecificType
            assertEquals("Any()", specificType.type)
        }
    }

    // Helper function for creating test type analysis objects
    private fun createTypeAnalysis(
        qualifiedName: String,
        isNullable: Boolean,
        generics: List<TypeAnalysis> = emptyList()
    ): TypeAnalysis {
        return TypeAnalysis(
            qualifiedName = qualifiedName,
            isNullable = isNullable,
            genericArguments = generics,
            isBuiltin = qualifiedName.startsWith("kotlin.")
        )
    }
}
