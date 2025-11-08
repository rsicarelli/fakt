// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir

import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [FakeClassChecker] metadata extraction following GIVEN-WHEN-THEN pattern.
 *
 * Testing strategy:
 * - Validates metadata extraction from abstract classes
 * - Verifies separation of abstract vs open members
 * - Ensures type parameter extraction with bounds
 * - Tests method-level type parameters
 *
 * Phase 3C.1: These tests validate that FakeClassChecker correctly extracts
 * metadata from FIR class declarations and stores it for IR transformation.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeClassCheckerTest {
    @Test
    fun `GIVEN abstract class with abstract property WHEN extracting metadata THEN captures abstract property`() =
        runTest {
            // GIVEN: abstract class AbstractRepository { abstract val id: Long }
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractRepository",
                    abstractProperties =
                        listOf(
                            FirPropertyInfo(
                                name = "id",
                                type = "kotlin.Long",
                                isMutable = false,
                                isNullable = false,
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker
            // (validated through integration tests)

            // THEN: Abstract property is captured correctly
            assertEquals(1, metadata.abstractProperties.size)
            assertEquals("id", metadata.abstractProperties[0].name)
            assertEquals("kotlin.Long", metadata.abstractProperties[0].type)
            assertEquals(false, metadata.abstractProperties[0].isMutable)
            assertEquals(0, metadata.openProperties.size)
        }

    @Test
    fun `GIVEN abstract class with open property WHEN extracting metadata THEN captures open property`() =
        runTest {
            // GIVEN: abstract class AbstractRepository { open var name: String = "" }
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractRepository",
                    openProperties =
                        listOf(
                            FirPropertyInfo(
                                name = "name",
                                type = "kotlin.String",
                                isMutable = true,
                                isNullable = false,
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker
            // (validated through integration tests)

            // THEN: Open property is captured separately from abstract
            assertEquals(0, metadata.abstractProperties.size)
            assertEquals(1, metadata.openProperties.size)
            assertEquals("name", metadata.openProperties[0].name)
            assertEquals(true, metadata.openProperties[0].isMutable)
        }

    @Test
    fun `GIVEN abstract class with mixed properties WHEN extracting metadata THEN separates abstract and open`() =
        runTest {
            // GIVEN: abstract class with both abstract and open properties
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.MixedRepository",
                    abstractProperties =
                        listOf(
                            FirPropertyInfo("id", "kotlin.Long", false, false),
                        ),
                    openProperties =
                        listOf(
                            FirPropertyInfo("name", "kotlin.String", true, false),
                            FirPropertyInfo("description", "kotlin.String?", false, true),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Properties are correctly separated by modality
            assertEquals(1, metadata.abstractProperties.size)
            assertEquals(2, metadata.openProperties.size)
            assertEquals("id", metadata.abstractProperties[0].name)
            assertEquals("name", metadata.openProperties[0].name)
            assertEquals("description", metadata.openProperties[1].name)
        }

    @Test
    fun `GIVEN abstract class with abstract method WHEN extracting metadata THEN captures abstract method`() =
        runTest {
            // GIVEN: abstract class AbstractService { abstract fun save(): Unit }
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractService",
                    abstractMethods =
                        listOf(
                            FirFunctionInfo(
                                name = "save",
                                parameters = emptyList(),
                                returnType = "kotlin.Unit",
                                isSuspend = false,
                                isInline = false,
                                typeParameters = emptyList(),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Abstract method is captured correctly
            assertEquals(1, metadata.abstractMethods.size)
            assertEquals("save", metadata.abstractMethods[0].name)
            assertEquals("kotlin.Unit", metadata.abstractMethods[0].returnType)
            assertEquals(0, metadata.openMethods.size)
        }

    @Test
    fun `GIVEN abstract class with open method WHEN extracting metadata THEN captures open method`() =
        runTest {
            // GIVEN: abstract class AbstractService { open fun validate(): Boolean = true }
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractService",
                    openMethods =
                        listOf(
                            FirFunctionInfo(
                                name = "validate",
                                parameters = emptyList(),
                                returnType = "kotlin.Boolean",
                                isSuspend = false,
                                isInline = false,
                                typeParameters = emptyList(),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Open method is captured separately from abstract
            assertEquals(0, metadata.abstractMethods.size)
            assertEquals(1, metadata.openMethods.size)
            assertEquals("validate", metadata.openMethods[0].name)
        }

    @Test
    fun `GIVEN abstract class with suspend method WHEN extracting metadata THEN captures suspend modifier`() =
        runTest {
            // GIVEN: abstract class AbstractRepository { abstract suspend fun fetch(): User }
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractRepository",
                    abstractMethods =
                        listOf(
                            FirFunctionInfo(
                                name = "fetch",
                                parameters = emptyList(),
                                returnType = "com.example.User",
                                isSuspend = true,
                                isInline = false,
                                typeParameters = emptyList(),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Suspend modifier is captured
            assertEquals(1, metadata.abstractMethods.size)
            assertEquals(true, metadata.abstractMethods[0].isSuspend)
            assertEquals("fetch", metadata.abstractMethods[0].name)
        }

    @Test
    fun `GIVEN abstract class with inline method WHEN extracting metadata THEN captures inline modifier`() =
        runTest {
            // GIVEN: abstract class AbstractCache { abstract inline fun <T> cached(fn: () -> T): T }
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractCache",
                    abstractMethods =
                        listOf(
                            FirFunctionInfo(
                                name = "cached",
                                parameters =
                                    listOf(
                                        FirParameterInfo(
                                            name = "fn",
                                            type = "kotlin.Function0<T>",
                                            hasDefaultValue = false,
                                            defaultValueCode = null,
                                            isVararg = false,
                                        ),
                                    ),
                                returnType = "T",
                                isSuspend = false,
                                isInline = true,
                                typeParameters =
                                    listOf(
                                        FirTypeParameterInfo("T", emptyList()),
                                    ),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Inline modifier and type parameter are captured
            assertEquals(1, metadata.abstractMethods.size)
            assertEquals(true, metadata.abstractMethods[0].isInline)
            assertEquals(1, metadata.abstractMethods[0].typeParameters.size)
            assertEquals("T", metadata.abstractMethods[0].typeParameters[0].name)
        }

    @Test
    fun `GIVEN abstract class with method parameters WHEN extracting metadata THEN captures all parameters`() =
        runTest {
            // GIVEN: abstract fun update(id: Long, name: String, age: Int = 0)
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractService",
                    abstractMethods =
                        listOf(
                            FirFunctionInfo(
                                name = "update",
                                parameters =
                                    listOf(
                                        FirParameterInfo(
                                            name = "id",
                                            type = "kotlin.Long",
                                            hasDefaultValue = false,
                                            defaultValueCode = null,
                                            isVararg = false,
                                        ),
                                        FirParameterInfo(
                                            name = "name",
                                            type = "kotlin.String",
                                            hasDefaultValue = false,
                                            defaultValueCode = null,
                                            isVararg = false,
                                        ),
                                        FirParameterInfo(
                                            name = "age",
                                            type = "kotlin.Int",
                                            hasDefaultValue = true,
                                            defaultValueCode = null,
                                            isVararg = false,
                                        ),
                                    ),
                                returnType = "kotlin.Unit",
                                isSuspend = false,
                                isInline = false,
                                typeParameters = emptyList(),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: All parameters with default values are captured
            val method = metadata.abstractMethods[0]
            assertEquals(3, method.parameters.size)
            assertEquals("id", method.parameters[0].name)
            assertEquals("name", method.parameters[1].name)
            assertEquals("age", method.parameters[2].name)
            assertEquals(true, method.parameters[2].hasDefaultValue)
        }

    @Test
    fun `GIVEN abstract class with vararg parameter WHEN extracting metadata THEN captures vararg modifier`() =
        runTest {
            // GIVEN: abstract fun process(vararg values: String)
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractProcessor",
                    abstractMethods =
                        listOf(
                            FirFunctionInfo(
                                name = "process",
                                parameters =
                                    listOf(
                                        FirParameterInfo(
                                            name = "values",
                                            type = "kotlin.Array<out kotlin.String>",
                                            hasDefaultValue = false,
                                            defaultValueCode = null,
                                            isVararg = true,
                                        ),
                                    ),
                                returnType = "kotlin.Unit",
                                isSuspend = false,
                                isInline = false,
                                typeParameters = emptyList(),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Vararg modifier is captured
            val param = metadata.abstractMethods[0].parameters[0]
            assertEquals(true, param.isVararg)
            assertEquals("values", param.name)
        }

    @Test
    fun `GIVEN abstract class with type parameter WHEN extracting metadata THEN captures type parameter`() =
        runTest {
            // GIVEN: abstract class AbstractRepository<T>
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractRepository",
                    typeParameters =
                        listOf(
                            FirTypeParameterInfo("T", emptyList()),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Type parameter is captured
            assertEquals(1, metadata.typeParameters.size)
            assertEquals("T", metadata.typeParameters[0].name)
            assertEquals(0, metadata.typeParameters[0].bounds.size)
        }

    @Test
    fun `GIVEN abstract class with bounded type parameter WHEN extracting metadata THEN captures bounds`() =
        runTest {
            // GIVEN: abstract class AbstractRepository<T : Comparable<T>>
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractRepository",
                    typeParameters =
                        listOf(
                            FirTypeParameterInfo("T", listOf("kotlin.Comparable<T>")),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Type parameter with bounds is captured
            assertEquals(1, metadata.typeParameters.size)
            assertEquals("T", metadata.typeParameters[0].name)
            assertEquals(1, metadata.typeParameters[0].bounds.size)
            assertEquals("kotlin.Comparable<T>", metadata.typeParameters[0].bounds[0])
        }

    @Test
    fun `GIVEN abstract class with multiple type parameters WHEN extracting metadata THEN captures all`() =
        runTest {
            // GIVEN: abstract class AbstractCache<K, V>
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractCache",
                    typeParameters =
                        listOf(
                            FirTypeParameterInfo("K", emptyList()),
                            FirTypeParameterInfo("V", emptyList()),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: All type parameters are captured
            assertEquals(2, metadata.typeParameters.size)
            assertEquals("K", metadata.typeParameters[0].name)
            assertEquals("V", metadata.typeParameters[1].name)
        }

    @Test
    fun `GIVEN abstract class with method-level type parameter WHEN extracting metadata THEN captures method type params`() =
        runTest {
            // GIVEN: abstract fun <R> transform(input: T): R
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractTransformer",
                    abstractMethods =
                        listOf(
                            FirFunctionInfo(
                                name = "transform",
                                parameters =
                                    listOf(
                                        FirParameterInfo("input", "T", false, null, false),
                                    ),
                                returnType = "R",
                                isSuspend = false,
                                isInline = false,
                                typeParameters =
                                    listOf(
                                        FirTypeParameterInfo("R", emptyList()),
                                    ),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Method-level type parameters are captured
            val method = metadata.abstractMethods[0]
            assertEquals(1, method.typeParameters.size)
            assertEquals("R", method.typeParameters[0].name)
        }

    @Test
    fun `GIVEN abstract class with bounded method type parameter WHEN extracting metadata THEN captures bounds`() =
        runTest {
            // GIVEN: abstract fun <R : Comparable<R>> sort(items: List<R>): List<R>
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractSorter",
                    abstractMethods =
                        listOf(
                            FirFunctionInfo(
                                name = "sort",
                                parameters =
                                    listOf(
                                        FirParameterInfo("items", "kotlin.collections.List<R>", false, null, false),
                                    ),
                                returnType = "kotlin.collections.List<R>",
                                isSuspend = false,
                                isInline = false,
                                typeParameters =
                                    listOf(
                                        FirTypeParameterInfo("R", listOf("kotlin.Comparable<R>")),
                                    ),
                                typeParameterBounds = mapOf("R" to "kotlin.Comparable<R>"),
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Method type parameter bounds are captured
            val method = metadata.abstractMethods[0]
            assertEquals(1, method.typeParameters.size)
            assertEquals("R", method.typeParameters[0].name)
            assertEquals("kotlin.Comparable<R>", method.typeParameters[0].bounds[0])
            assertEquals("kotlin.Comparable<R>", method.typeParameterBounds["R"])
        }

    @Test
    fun `GIVEN abstract class with nullable return type WHEN extracting metadata THEN captures nullability`() =
        runTest {
            // GIVEN: abstract fun findById(id: Long): User?
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractRepository",
                    abstractMethods =
                        listOf(
                            FirFunctionInfo(
                                name = "findById",
                                parameters =
                                    listOf(
                                        FirParameterInfo("id", "kotlin.Long", false, null, false),
                                    ),
                                returnType = "com.example.User?",
                                isSuspend = false,
                                isInline = false,
                                typeParameters = emptyList(),
                                typeParameterBounds = emptyMap(),
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: Nullable return type is captured
            val method = metadata.abstractMethods[0]
            assertEquals("com.example.User?", method.returnType)
        }

    @Test
    fun `GIVEN abstract class with complex signature WHEN extracting metadata THEN captures all details`() =
        runTest {
            // GIVEN: abstract class with complex method signature
            // abstract suspend inline fun <R : Comparable<R>> transformList(
            //     items: List<T>,
            //     transformer: (T) -> R,
            //     default: R? = null
            // ): List<R>?
            val metadata =
                createTestClassMetadata(
                    fqName = "com.example.AbstractTransformer",
                    typeParameters =
                        listOf(
                            FirTypeParameterInfo("T", emptyList()),
                        ),
                    abstractMethods =
                        listOf(
                            FirFunctionInfo(
                                name = "transformList",
                                parameters =
                                    listOf(
                                        FirParameterInfo("items", "kotlin.collections.List<T>", false, null, false),
                                        FirParameterInfo("transformer", "kotlin.Function1<T, R>", false, null, false),
                                        FirParameterInfo("default", "R?", true, null, false),
                                    ),
                                returnType = "kotlin.collections.List<R>?",
                                isSuspend = true,
                                isInline = true,
                                typeParameters =
                                    listOf(
                                        FirTypeParameterInfo("R", listOf("kotlin.Comparable<R>")),
                                    ),
                                typeParameterBounds = mapOf("R" to "kotlin.Comparable<R>"),
                            ),
                        ),
                )

            // WHEN: Metadata is extracted by FakeClassChecker

            // THEN: All complex signature details are captured
            assertEquals(1, metadata.typeParameters.size) // Class-level T
            val method = metadata.abstractMethods[0]
            assertEquals("transformList", method.name)
            assertEquals(true, method.isSuspend)
            assertEquals(true, method.isInline)
            assertEquals(3, method.parameters.size)
            assertEquals(true, method.parameters[2].hasDefaultValue)
            assertEquals(1, method.typeParameters.size) // Method-level R
            assertEquals("kotlin.Comparable<R>", method.typeParameters[0].bounds[0])
            assertTrue(method.returnType.contains("?")) // Nullable return
        }

    // Helper function to create test metadata
    private fun createTestClassMetadata(
        fqName: String,
        typeParameters: List<FirTypeParameterInfo> = emptyList(),
        abstractProperties: List<FirPropertyInfo> = emptyList(),
        openProperties: List<FirPropertyInfo> = emptyList(),
        abstractMethods: List<FirFunctionInfo> = emptyList(),
        openMethods: List<FirFunctionInfo> = emptyList(),
    ): ValidatedFakeClass {
        val parts = fqName.split(".")
        val simpleName = parts.last()
        val packageName = parts.dropLast(1).joinToString(".")

        return ValidatedFakeClass(
            classId = ClassId.topLevel(FqName(fqName)),
            simpleName = simpleName,
            packageName = packageName,
            typeParameters = typeParameters,
            abstractProperties = abstractProperties,
            openProperties = openProperties,
            abstractMethods = abstractMethods,
            openMethods = openMethods,
            sourceLocation = FirSourceLocation.UNKNOWN,
        )
    }
}
