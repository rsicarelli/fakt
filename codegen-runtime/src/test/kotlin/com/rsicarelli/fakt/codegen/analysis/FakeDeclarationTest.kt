// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.analysis

import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Structural smoke tests for the pure analysis contract introduced in 3.1.d.3.
 *
 * Generators will exercise these types end-to-end once they relocate to `:codegen-runtime` in
 * 3.1.d.4. These tests just verify the records expose the right fields, default flags behave as
 * documented, and the sealed hierarchy is exhaustive enough for a `when` consumer.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeDeclarationTest {
    @Test
    fun `GIVEN Interface variant WHEN constructed THEN exposes shared fields via sealed interface`() {
        // GIVEN/WHEN
        val declaration: FakeDeclaration =
            FakeDeclaration.Interface(
                simpleName = "UserRepository",
                qualifiedSourceName = "UserRepository",
                packageName = "com.example",
                typeParameters = emptyList(),
                visibility = FirVisibility.PUBLIC,
                annotations = emptyList(),
                requiredImports = emptySet(),
                generateCallHistory = true,
                generateMutableBehaviors = false,
                genericPattern = PureGenericPattern.NoGenerics,
                properties = emptyList(),
                functions = emptyList(),
            )

        // THEN
        assertEquals("UserRepository", declaration.simpleName)
        assertEquals("com.example", declaration.packageName)
        assertEquals(FirVisibility.PUBLIC, declaration.visibility)
        assertTrue(declaration.generateCallHistory)
        assertFalse(declaration.generateMutableBehaviors)
    }

    @Test
    fun `GIVEN Class variant WHEN constructed THEN exposes shared fields via sealed interface`() {
        // GIVEN/WHEN
        val declaration: FakeDeclaration =
            FakeDeclaration.Class(
                simpleName = "BaseService",
                qualifiedSourceName = "BaseService",
                packageName = "com.example",
                typeParameters = listOf("T"),
                visibility = FirVisibility.INTERNAL,
                annotations = emptyList(),
                requiredImports = setOf("kotlin.String"),
                generateCallHistory = false,
                generateMutableBehaviors = true,
                constructorParameters = emptyList(),
                abstractMethods = emptyList(),
                openMethods = emptyList(),
                abstractProperties = emptyList(),
                openProperties = emptyList(),
            )

        // THEN
        assertEquals("BaseService", declaration.simpleName)
        assertEquals(listOf("T"), declaration.typeParameters)
        assertEquals(FirVisibility.INTERNAL, declaration.visibility)
        assertEquals(setOf("kotlin.String"), declaration.requiredImports)
        assertFalse(declaration.generateCallHistory)
        assertTrue(declaration.generateMutableBehaviors)
    }

    @Test
    fun `GIVEN sealed FakeDeclaration WHEN switching variants THEN both branches are reachable`() {
        // GIVEN
        val declarations: List<FakeDeclaration> =
            listOf(
                FakeDeclaration.Interface(
                    simpleName = "I",
                    qualifiedSourceName = "I",
                    packageName = "p",
                    typeParameters = emptyList(),
                    visibility = FirVisibility.PUBLIC,
                    annotations = emptyList(),
                    requiredImports = emptySet(),
                    generateCallHistory = true,
                    generateMutableBehaviors = false,
                    genericPattern = PureGenericPattern.NoGenerics,
                    properties = emptyList(),
                    functions = emptyList(),
                ),
                FakeDeclaration.Class(
                    simpleName = "C",
                    qualifiedSourceName = "C",
                    packageName = "p",
                    typeParameters = emptyList(),
                    visibility = FirVisibility.PUBLIC,
                    annotations = emptyList(),
                    requiredImports = emptySet(),
                    generateCallHistory = true,
                    generateMutableBehaviors = false,
                    constructorParameters = emptyList(),
                    abstractMethods = emptyList(),
                    openMethods = emptyList(),
                    abstractProperties = emptyList(),
                    openProperties = emptyList(),
                ),
            )

        // WHEN
        val kinds =
            declarations.map { declaration ->
                when (declaration) {
                    is FakeDeclaration.Interface -> "interface"
                    is FakeDeclaration.Class -> "class"
                }
            }

        // THEN
        assertEquals(listOf("interface", "class"), kinds)
    }

    @Test
    fun `GIVEN PropertySpec without semantic flags WHEN constructed THEN flags default to false`() {
        // GIVEN/WHEN
        val spec =
            PropertySpec(
                name = "name",
                typeString = "String",
                isMutable = false,
                isNullable = false,
            )

        // THEN
        assertFalse(spec.isTypeParameter)
        assertFalse(spec.requiresCollectionErasure)
    }

    @Test
    fun `GIVEN ParameterSpec without semantic flags WHEN constructed THEN flags default to false`() {
        // GIVEN/WHEN
        val spec =
            ParameterSpec(
                name = "id",
                typeString = "Int",
                hasDefaultValue = false,
                defaultValueCode = null,
                isVararg = false,
            )

        // THEN
        assertFalse(spec.isTypeParameter)
        assertFalse(spec.requiresCollectionErasure)
    }

    @Test
    fun `GIVEN DeclarationAnnotation without optional fields WHEN constructed THEN defaults apply`() {
        // GIVEN/WHEN
        val annotation =
            DeclarationAnnotation(
                simpleName = "Deprecated",
                fullyQualifiedName = "kotlin.Deprecated",
            )

        // THEN
        assertEquals(emptyList(), annotation.renderedArguments)
        assertFalse(annotation.isOptInMarker)
    }

    @Test
    fun `GIVEN PureGenericPattern variants WHEN matched exhaustively THEN all four are reachable`() {
        // GIVEN
        val patterns: List<PureGenericPattern> =
            listOf(
                PureGenericPattern.NoGenerics,
                PureGenericPattern.ClassLevelGenerics(typeParameterNames = listOf("T")),
                PureGenericPattern.MethodLevelGenerics(methodNames = listOf("transform")),
                PureGenericPattern.MixedGenerics(
                    typeParameterNames = listOf("T"),
                    methodNames = listOf("transform"),
                ),
            )

        // WHEN
        val labels =
            patterns.map { pattern ->
                when (pattern) {
                    is PureGenericPattern.NoGenerics -> "none"
                    is PureGenericPattern.ClassLevelGenerics -> "class"
                    is PureGenericPattern.MethodLevelGenerics -> "method"
                    is PureGenericPattern.MixedGenerics -> "mixed"
                }
            }

        // THEN
        assertEquals(listOf("none", "class", "method", "mixed"), labels)
    }

    @Test
    fun `GIVEN MixedGenerics WHEN constructed THEN exposes both type-parameter and method names`() {
        // GIVEN/WHEN
        val pattern =
            PureGenericPattern.MixedGenerics(
                typeParameterNames = listOf("T", "R"),
                methodNames = listOf("map", "flatMap"),
            )

        // THEN
        assertEquals(listOf("T", "R"), pattern.typeParameterNames)
        assertEquals(listOf("map", "flatMap"), pattern.methodNames)
    }

    @Test
    fun `GIVEN ConstructorParam WHEN constructed THEN fields are accessible`() {
        // GIVEN/WHEN
        val param = ConstructorParam(name = "logger", typeString = "Logger", hasDefault = true)

        // THEN
        assertEquals("logger", param.name)
        assertEquals("Logger", param.typeString)
        assertTrue(param.hasDefault)
    }
}
