// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.fir.metadata

import kotlinx.coroutines.test.runTest
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for FIR metadata data classes following GIVEN-WHEN-THEN pattern.
 *
 * Testing strategy:
 * - Data class immutability
 * - FirSourceLocation formatting
 * - Metadata structure correctness
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FirFakeMetadataTest {
    @Test
    fun `GIVEN FirSourceLocation WHEN converting to display string THEN formats correctly`() =
        runTest {
            // GIVEN
            val location =
                FirSourceLocation(
                    filePath = "src/main/kotlin/UserService.kt",
                    startLine = 42,
                    startColumn = 15,
                    endLine = 42,
                    endColumn = 27,
                )

            // WHEN
            val displayString = location.toDisplayString()

            // THEN
            assertEquals("src/main/kotlin/UserService.kt:42:15", displayString)
        }

    @Test
    fun `GIVEN UNKNOWN source location WHEN converting to display string THEN shows unknown`() =
        runTest {
            // GIVEN
            val location = FirSourceLocation.UNKNOWN

            // WHEN
            val displayString = location.toDisplayString()

            // THEN
            assertEquals("<unknown>:0:0", displayString)
        }

    @Test
    fun `GIVEN ValidatedFakeInterface WHEN created THEN contains all metadata`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.UserService"))
            val typeParams =
                listOf(
                    FirTypeParameterInfo("T", emptyList()),
                    FirTypeParameterInfo("R", listOf("Comparable<R>")),
                )
            val properties =
                listOf(
                    FirPropertyInfo(
                        name = "userId",
                        type = "String",
                        isMutable = false,
                        isNullable = false,
                    ),
                )
            val functions =
                listOf(
                    FirFunctionInfo(
                        name = "getUser",
                        parameters = emptyList(),
                        returnType = "T",
                        isSuspend = true,
                        isInline = false,
                        typeParameters = emptyList(),
                        typeParameterBounds = emptyMap(),
                    ),
                )

            // WHEN
            val metadata =
                ValidatedFakeInterface(
                    classId = classId,
                    simpleName = "UserService",
                    packageName = "com.example",
                    typeParameters = typeParams,
                    properties = properties,
                    functions = functions,
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                )

            // THEN
            assertEquals(classId, metadata.classId)
            assertEquals("UserService", metadata.simpleName)
            assertEquals("com.example", metadata.packageName)
            assertEquals(2, metadata.typeParameters.size)
            assertEquals(1, metadata.properties.size)
            assertEquals(1, metadata.functions.size)
        }

    @Test
    fun `GIVEN FirTypeParameterInfo with bounds WHEN created THEN stores bounds correctly`() =
        runTest {
            // GIVEN & WHEN
            val typeParam =
                FirTypeParameterInfo(
                    name = "T",
                    bounds = listOf("Comparable<T>", "Serializable"),
                )

            // THEN
            assertEquals("T", typeParam.name)
            assertEquals(2, typeParam.bounds.size)
            assertEquals("Comparable<T>", typeParam.bounds[0])
            assertEquals("Serializable", typeParam.bounds[1])
        }

    @Test
    fun `GIVEN FirPropertyInfo WHEN created THEN captures all property details`() =
        runTest {
            // GIVEN & WHEN
            val propertyInfo =
                FirPropertyInfo(
                    name = "userName",
                    type = "String?",
                    isMutable = true,
                    isNullable = true,
                )

            // THEN
            assertEquals("userName", propertyInfo.name)
            assertEquals("String?", propertyInfo.type)
            assertEquals(true, propertyInfo.isMutable)
            assertEquals(true, propertyInfo.isNullable)
        }

    @Test
    fun `GIVEN FirFunctionInfo with suspend WHEN created THEN captures suspend modifier`() =
        runTest {
            // GIVEN & WHEN
            val functionInfo =
                FirFunctionInfo(
                    name = "fetchUser",
                    parameters =
                        listOf(
                            FirParameterInfo(
                                name = "id",
                                type = "String",
                                hasDefaultValue = false,
                                defaultValueCode = null,
                                isVararg = false,
                            ),
                        ),
                    returnType = "User",
                    isSuspend = true,
                    isInline = false,
                    typeParameters = emptyList(),
                    typeParameterBounds = emptyMap(),
                )

            // THEN
            assertEquals("fetchUser", functionInfo.name)
            assertEquals(true, functionInfo.isSuspend)
            assertEquals(false, functionInfo.isInline)
            assertEquals("User", functionInfo.returnType)
            assertEquals(1, functionInfo.parameters.size)
        }

    @Test
    fun `GIVEN FirFunctionInfo with type parameters WHEN created THEN captures type param bounds`() =
        runTest {
            // GIVEN & WHEN
            val functionInfo =
                FirFunctionInfo(
                    name = "transform",
                    parameters = emptyList(),
                    returnType = "R",
                    isSuspend = false,
                    isInline = true,
                    typeParameters =
                        listOf(
                            FirTypeParameterInfo("T", emptyList()),
                            FirTypeParameterInfo("R", emptyList()),
                        ),
                    typeParameterBounds = mapOf("R" to "Comparable<R>"),
                )

            // THEN
            assertEquals(2, functionInfo.typeParameters.size)
            assertEquals("Comparable<R>", functionInfo.typeParameterBounds["R"])
            assertEquals(true, functionInfo.isInline)
        }

    @Test
    fun `GIVEN FirParameterInfo with vararg WHEN created THEN captures vararg modifier`() =
        runTest {
            // GIVEN & WHEN
            val parameterInfo =
                FirParameterInfo(
                    name = "values",
                    type = "String",
                    hasDefaultValue = false,
                    defaultValueCode = null,
                    isVararg = true,
                )

            // THEN
            assertEquals("values", parameterInfo.name)
            assertEquals(true, parameterInfo.isVararg)
            assertEquals(false, parameterInfo.hasDefaultValue)
        }

    @Test
    fun `GIVEN ValidatedFakeClass WHEN created THEN separates abstract and open members`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.AbstractRepository"))
            val abstractProps =
                listOf(
                    FirPropertyInfo("id", "Long", false, false),
                )
            val openProps =
                listOf(
                    FirPropertyInfo("name", "String", true, false),
                )
            val abstractMethods =
                listOf(
                    FirFunctionInfo(
                        name = "save",
                        parameters = emptyList(),
                        returnType = "Unit",
                        isSuspend = false,
                        isInline = false,
                        typeParameters = emptyList(),
                        typeParameterBounds = emptyMap(),
                    ),
                )
            val openMethods =
                listOf(
                    FirFunctionInfo(
                        name = "validate",
                        parameters = emptyList(),
                        returnType = "Boolean",
                        isSuspend = false,
                        isInline = false,
                        typeParameters = emptyList(),
                        typeParameterBounds = emptyMap(),
                    ),
                )

            // WHEN
            val metadata =
                ValidatedFakeClass(
                    classId = classId,
                    simpleName = "AbstractRepository",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    abstractProperties = abstractProps,
                    openProperties = openProps,
                    abstractMethods = abstractMethods,
                    openMethods = openMethods,
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                )

            // THEN
            assertEquals(1, metadata.abstractProperties.size)
            assertEquals(1, metadata.openProperties.size)
            assertEquals(1, metadata.abstractMethods.size)
            assertEquals(1, metadata.openMethods.size)
            assertEquals("AbstractRepository", metadata.simpleName)
        }

    // ==========================================
    // FirVisibility.from() Tests (Issue #21)
    // ==========================================

    @Test
    fun `GIVEN Visibilities Public WHEN converting to FirVisibility THEN returns PUBLIC`() =
        runTest {
            // GIVEN
            val visibility = Visibilities.Public

            // WHEN
            val firVisibility = FirVisibility.from(visibility)

            // THEN
            assertEquals(FirVisibility.PUBLIC, firVisibility)
        }

    @Test
    fun `GIVEN Visibilities Internal WHEN converting to FirVisibility THEN returns INTERNAL`() =
        runTest {
            // GIVEN
            val visibility = Visibilities.Internal

            // WHEN
            val firVisibility = FirVisibility.from(visibility)

            // THEN
            assertEquals(FirVisibility.INTERNAL, firVisibility)
        }

    @Test
    fun `GIVEN Visibilities Private WHEN converting to FirVisibility THEN returns PRIVATE`() =
        runTest {
            // GIVEN
            val visibility = Visibilities.Private

            // WHEN
            val firVisibility = FirVisibility.from(visibility)

            // THEN
            assertEquals(FirVisibility.PRIVATE, firVisibility)
        }

    @Test
    fun `GIVEN Visibilities Protected WHEN converting to FirVisibility THEN returns PROTECTED`() =
        runTest {
            // GIVEN
            val visibility = Visibilities.Protected

            // WHEN
            val firVisibility = FirVisibility.from(visibility)

            // THEN
            assertEquals(FirVisibility.PROTECTED, firVisibility)
        }

    @Test
    fun `GIVEN FirVisibility toModifier WHEN PUBLIC THEN returns public with space`() =
        runTest {
            // GIVEN
            val visibility = FirVisibility.PUBLIC

            // WHEN
            val modifier = visibility.toModifier()

            // THEN
            assertEquals("public ", modifier)
        }

    @Test
    fun `GIVEN FirVisibility toModifier WHEN INTERNAL THEN returns internal with space`() =
        runTest {
            // GIVEN
            val visibility = FirVisibility.INTERNAL

            // WHEN
            val modifier = visibility.toModifier()

            // THEN
            assertEquals("internal ", modifier)
        }

    // ==========================================
    // Annotation Data Model Tests
    // ==========================================

    @Test
    fun `GIVEN FirAnnotationInfo with no arguments WHEN created THEN is valid marker annotation`() =
        runTest {
            // GIVEN & WHEN
            val annotation =
                FirAnnotationInfo(
                    annotationClassId = "kotlin.Deprecated",
                    arguments = emptyMap(),
                )

            // THEN
            assertEquals("kotlin.Deprecated", annotation.annotationClassId)
            assertTrue(annotation.arguments.isEmpty())
        }

    @Test
    fun `GIVEN FirAnnotationInfo with string argument WHEN created THEN captures argument`() =
        runTest {
            // GIVEN & WHEN
            val annotation =
                FirAnnotationInfo(
                    annotationClassId = "kotlin.Deprecated",
                    arguments =
                        mapOf(
                            "message" to FirAnnotationArgument.StringLiteral("Use newMethod instead"),
                        ),
                )

            // THEN
            assertEquals("kotlin.Deprecated", annotation.annotationClassId)
            assertEquals(1, annotation.arguments.size)
            val messageArg = annotation.arguments["message"]
            assertTrue(messageArg is FirAnnotationArgument.StringLiteral)
            assertEquals("Use newMethod instead", (messageArg as FirAnnotationArgument.StringLiteral).value)
        }

    @Test
    fun `GIVEN FirAnnotationInfo with class reference WHEN created THEN captures class ID`() =
        runTest {
            // GIVEN & WHEN - @OptIn(ExperimentalApi::class)
            val annotation =
                FirAnnotationInfo(
                    annotationClassId = "kotlin.OptIn",
                    arguments =
                        mapOf(
                            "markerClasses" to
                                FirAnnotationArgument.ArrayValue(
                                    elements =
                                        listOf(
                                            FirAnnotationArgument.ClassReference("com.example.ExperimentalApi"),
                                        ),
                                ),
                        ),
                )

            // THEN
            assertEquals("kotlin.OptIn", annotation.annotationClassId)
            val markerArg = annotation.arguments["markerClasses"]
            assertTrue(markerArg is FirAnnotationArgument.ArrayValue)
            val elements = (markerArg as FirAnnotationArgument.ArrayValue).elements
            assertEquals(1, elements.size)
            assertTrue(elements[0] is FirAnnotationArgument.ClassReference)
            assertEquals("com.example.ExperimentalApi", (elements[0] as FirAnnotationArgument.ClassReference).classId)
        }

    @Test
    fun `GIVEN FirAnnotationInfo with enum value WHEN created THEN captures enum entry`() =
        runTest {
            // GIVEN & WHEN - @Target(AnnotationTarget.CLASS)
            val annotation =
                FirAnnotationInfo(
                    annotationClassId = "kotlin.annotation.Target",
                    arguments =
                        mapOf(
                            "allowedTargets" to
                                FirAnnotationArgument.ArrayValue(
                                    elements =
                                        listOf(
                                            FirAnnotationArgument.EnumValue(
                                                enumClassId = "kotlin.annotation.AnnotationTarget",
                                                entryName = "CLASS",
                                            ),
                                        ),
                                ),
                        ),
                )

            // THEN
            assertEquals("kotlin.annotation.Target", annotation.annotationClassId)
            val targetArg = annotation.arguments["allowedTargets"]
            assertTrue(targetArg is FirAnnotationArgument.ArrayValue)
            val elements = (targetArg as FirAnnotationArgument.ArrayValue).elements
            assertEquals(1, elements.size)
            assertTrue(elements[0] is FirAnnotationArgument.EnumValue)
            val enumVal = elements[0] as FirAnnotationArgument.EnumValue
            assertEquals("kotlin.annotation.AnnotationTarget", enumVal.enumClassId)
            assertEquals("CLASS", enumVal.entryName)
        }

    @Test
    fun `GIVEN FirAnnotationArgument types WHEN created THEN all types work correctly`() =
        runTest {
            // GIVEN & WHEN - Test all argument types
            val stringArg = FirAnnotationArgument.StringLiteral("test")
            val numberArg = FirAnnotationArgument.NumberLiteral("42")
            val boolArg = FirAnnotationArgument.BooleanLiteral(true)
            val charArg = FirAnnotationArgument.CharLiteral('x')
            val classRefArg = FirAnnotationArgument.ClassReference("com.example.Foo")
            val enumArg = FirAnnotationArgument.EnumValue("com.example.MyEnum", "ENTRY")
            val arrayArg = FirAnnotationArgument.ArrayValue(listOf(stringArg, numberArg))
            val nestedArg =
                FirAnnotationArgument.NestedAnnotation(
                    FirAnnotationInfo("kotlin.Suppress", mapOf("names" to stringArg)),
                )

            // THEN
            assertEquals("test", stringArg.value)
            assertEquals("42", numberArg.value)
            assertEquals(true, boolArg.value)
            assertEquals('x', charArg.value)
            assertEquals("com.example.Foo", classRefArg.classId)
            assertEquals("com.example.MyEnum", enumArg.enumClassId)
            assertEquals("ENTRY", enumArg.entryName)
            assertEquals(2, arrayArg.elements.size)
            assertEquals("kotlin.Suppress", nestedArg.annotation.annotationClassId)
        }

    @Test
    fun `GIVEN ValidatedFakeInterface with annotations WHEN created THEN contains annotations`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.ExperimentalService"))
            val annotations =
                listOf(
                    FirAnnotationInfo(
                        annotationClassId = "kotlin.OptIn",
                        arguments =
                            mapOf(
                                "markerClasses" to
                                    FirAnnotationArgument.ArrayValue(
                                        listOf(
                                            FirAnnotationArgument.ClassReference("com.example.ExperimentalApi"),
                                        ),
                                    ),
                            ),
                    ),
                    FirAnnotationInfo(
                        annotationClassId = "kotlin.Deprecated",
                        arguments =
                            mapOf(
                                "message" to FirAnnotationArgument.StringLiteral("Use NewService instead"),
                            ),
                    ),
                )

            // WHEN
            val metadata =
                ValidatedFakeInterface(
                    classId = classId,
                    simpleName = "ExperimentalService",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    annotations = annotations,
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                )

            // THEN
            assertEquals(2, metadata.annotations.size)
            assertEquals("kotlin.OptIn", metadata.annotations[0].annotationClassId)
            assertEquals("kotlin.Deprecated", metadata.annotations[1].annotationClassId)
        }

    @Test
    fun `GIVEN ValidatedFakeClass with annotations WHEN created THEN contains annotations`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.DeprecatedRepository"))
            val annotations =
                listOf(
                    FirAnnotationInfo(
                        annotationClassId = "kotlin.Deprecated",
                        arguments =
                            mapOf(
                                "message" to FirAnnotationArgument.StringLiteral("Use NewRepository"),
                                "level" to
                                    FirAnnotationArgument.EnumValue(
                                        enumClassId = "kotlin.DeprecationLevel",
                                        entryName = "WARNING",
                                    ),
                            ),
                    ),
                )

            // WHEN
            val metadata =
                ValidatedFakeClass(
                    classId = classId,
                    simpleName = "DeprecatedRepository",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    abstractProperties = emptyList(),
                    openProperties = emptyList(),
                    abstractMethods = emptyList(),
                    openMethods = emptyList(),
                    annotations = annotations,
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                )

            // THEN
            assertEquals(1, metadata.annotations.size)
            assertEquals("kotlin.Deprecated", metadata.annotations[0].annotationClassId)
            assertEquals(2, metadata.annotations[0].arguments.size)
        }

    // ==========================================
    // FirCallHistoryMode Tests (Call History Control)
    // ==========================================

    @Test
    fun `GIVEN FirCallHistoryMode DEFAULT WHEN used THEN represents plugin default behavior`() =
        runTest {
            // GIVEN
            val mode = FirCallHistoryMode.DEFAULT

            // WHEN
            val name = mode.name

            // THEN
            assertEquals("DEFAULT", name)
        }

    @Test
    fun `GIVEN FirCallHistoryMode ENABLED WHEN used THEN forces call history generation`() =
        runTest {
            // GIVEN
            val mode = FirCallHistoryMode.ENABLED

            // WHEN
            val name = mode.name

            // THEN
            assertEquals("ENABLED", name)
        }

    @Test
    fun `GIVEN FirCallHistoryMode DISABLED WHEN used THEN disables call history generation`() =
        runTest {
            // GIVEN
            val mode = FirCallHistoryMode.DISABLED

            // WHEN
            val name = mode.name

            // THEN
            assertEquals("DISABLED", name)
        }

    @Test
    fun `GIVEN ValidatedFakeInterface WHEN created with callHistoryMode ENABLED THEN contains mode`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.TrackedService"))

            // WHEN
            val metadata =
                ValidatedFakeInterface(
                    classId = classId,
                    simpleName = "TrackedService",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                    callHistoryMode = FirCallHistoryMode.ENABLED,
                )

            // THEN
            assertEquals(FirCallHistoryMode.ENABLED, metadata.callHistoryMode)
        }

    @Test
    fun `GIVEN ValidatedFakeInterface WHEN created with callHistoryMode DISABLED THEN contains mode`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.LightweightService"))

            // WHEN
            val metadata =
                ValidatedFakeInterface(
                    classId = classId,
                    simpleName = "LightweightService",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                    callHistoryMode = FirCallHistoryMode.DISABLED,
                )

            // THEN
            assertEquals(FirCallHistoryMode.DISABLED, metadata.callHistoryMode)
        }

    @Test
    fun `GIVEN ValidatedFakeInterface WHEN created without callHistoryMode THEN defaults to DEFAULT`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.DefaultService"))

            // WHEN
            val metadata =
                ValidatedFakeInterface(
                    classId = classId,
                    simpleName = "DefaultService",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    properties = emptyList(),
                    functions = emptyList(),
                    inheritedProperties = emptyList(),
                    inheritedFunctions = emptyList(),
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                )

            // THEN
            assertEquals(FirCallHistoryMode.DEFAULT, metadata.callHistoryMode)
        }

    @Test
    fun `GIVEN ValidatedFakeClass WHEN created with callHistoryMode ENABLED THEN contains mode`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.TrackedRepository"))

            // WHEN
            val metadata =
                ValidatedFakeClass(
                    classId = classId,
                    simpleName = "TrackedRepository",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    abstractProperties = emptyList(),
                    openProperties = emptyList(),
                    abstractMethods = emptyList(),
                    openMethods = emptyList(),
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                    callHistoryMode = FirCallHistoryMode.ENABLED,
                )

            // THEN
            assertEquals(FirCallHistoryMode.ENABLED, metadata.callHistoryMode)
        }

    @Test
    fun `GIVEN ValidatedFakeClass WHEN created with callHistoryMode DISABLED THEN contains mode`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.LightweightRepository"))

            // WHEN
            val metadata =
                ValidatedFakeClass(
                    classId = classId,
                    simpleName = "LightweightRepository",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    abstractProperties = emptyList(),
                    openProperties = emptyList(),
                    abstractMethods = emptyList(),
                    openMethods = emptyList(),
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                    callHistoryMode = FirCallHistoryMode.DISABLED,
                )

            // THEN
            assertEquals(FirCallHistoryMode.DISABLED, metadata.callHistoryMode)
        }

    @Test
    fun `GIVEN ValidatedFakeClass WHEN created without callHistoryMode THEN defaults to DEFAULT`() =
        runTest {
            // GIVEN
            val classId = ClassId.topLevel(FqName("com.example.DefaultRepository"))

            // WHEN
            val metadata =
                ValidatedFakeClass(
                    classId = classId,
                    simpleName = "DefaultRepository",
                    packageName = "com.example",
                    typeParameters = emptyList(),
                    abstractProperties = emptyList(),
                    openProperties = emptyList(),
                    abstractMethods = emptyList(),
                    openMethods = emptyList(),
                    sourceLocation = FirSourceLocation.UNKNOWN,
                    validationTimeNanos = 0L,
                )

            // THEN
            assertEquals(FirCallHistoryMode.DEFAULT, metadata.callHistoryMode)
        }
}
