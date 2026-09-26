// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.analysis

import com.rsicarelli.fakt.codegen.FaktCodegen
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OverloadNamingTest {
    @Test
    fun `GIVEN no overloads WHEN withUniqueOverloadNames THEN names are unchanged`() {
        // GIVEN
        val decl = interfaceDecl(function("getUser", param("id", "String")), function("logout"))

        // WHEN
        val result = decl.withUniqueOverloadNames() as FakeDeclaration.Interface

        // THEN
        assertEquals(listOf("getUser", "logout"), result.functions.map { it.name })
        assertEquals(listOf("getUser", "logout"), result.functions.map { it.sourceName })
    }

    @Test
    fun `GIVEN overloads WHEN withUniqueOverloadNames THEN names use parameter type suffixes`() {
        // GIVEN
        val decl =
            interfaceDecl(
                function("find", param("id", "Int")),
                function("find", param("name", "kotlin.String?")),
                function("find"),
            )

        // WHEN
        val result = decl.withUniqueOverloadNames() as FakeDeclaration.Interface

        // THEN
        assertEquals(listOf("findInt", "findString", "find"), result.functions.map { it.name })
        assertEquals(listOf("find", "find", "find"), result.functions.map { it.sourceName })
    }

    @Test
    fun `GIVEN overloads from issue 146 WHEN withUniqueOverloadNames THEN generic params become Any`() {
        // GIVEN
        val decl =
            interfaceDecl(
                function(
                    "setLocalOverride",
                    param("feature", "Feature"),
                    param("override", "T?"),
                    typeParameters = listOf("T"),
                ),
                function(
                    "setLocalOverride",
                    param("abTest", "ABTest<*>"),
                    param("override", "String?"),
                ),
            )

        // WHEN
        val result = decl.withUniqueOverloadNames() as FakeDeclaration.Interface

        // THEN
        assertEquals(
            listOf("setLocalOverrideFeatureAny", "setLocalOverrideABTestString"),
            result.functions.map { it.name },
        )
    }

    @Test
    fun `GIVEN overloads with function vararg and receiver types WHEN withUniqueOverloadNames THEN suffixes describe them`() {
        // GIVEN
        val decl =
            interfaceDecl(
                function("run", param("block", "suspend (Int) -> Unit")),
                function("run", param("items", "Array<out String>", isVararg = true)),
                function("run", receiver = "Context"),
            )

        // WHEN
        val result = decl.withUniqueOverloadNames() as FakeDeclaration.Interface

        // THEN
        assertEquals(
            listOf("runFunction", "runVarargString", "runContext"),
            result.functions.map { it.name },
        )
    }

    @Test
    fun `GIVEN suffixed name already used by another member WHEN withUniqueOverloadNames THEN appends counter`() {
        // GIVEN
        val decl =
            interfaceDecl(
                function("find", param("id", "Int")),
                function("find", param("ids", "List<Int>")),
                function("findInt"),
            )

        // WHEN
        val result = decl.withUniqueOverloadNames() as FakeDeclaration.Interface

        // THEN
        assertEquals(listOf("findInt2", "findList", "findInt"), result.functions.map { it.name })
    }

    @Test
    fun `GIVEN overloads with same erased simple types WHEN withUniqueOverloadNames THEN appends counter`() {
        // GIVEN
        val decl =
            interfaceDecl(
                function("save", param("items", "List<String>")),
                function("save", param("items", "List<Int>")),
            )

        // WHEN
        val result = decl.withUniqueOverloadNames() as FakeDeclaration.Interface

        // THEN
        assertEquals(listOf("saveList", "saveList2"), result.functions.map { it.name })
    }

    @Test
    fun `GIVEN class with abstract and open overloads WHEN withUniqueOverloadNames THEN renames across both lists`() {
        // GIVEN
        val decl =
            classDecl(
                abstractMethods = listOf(function("count", param("prefix", "String"))),
                openMethods = listOf(function("count")),
            )

        // WHEN
        val result = decl.withUniqueOverloadNames() as FakeDeclaration.Class

        // THEN
        assertEquals(listOf("countString"), result.abstractMethods.map { it.name })
        assertEquals(listOf("count"), result.openMethods.map { it.name })
    }

    @Test
    fun `GIVEN interface with overloads WHEN render THEN overrides keep source name and members are unique`() {
        // GIVEN
        val decl =
            interfaceDecl(
                function("find", param("id", "Int"), returnType = "String"),
                function("find", param("name", "String"), returnType = "String"),
                callHistory = true,
            )

        // WHEN
        val content = FaktCodegen.render(decl).content

        // THEN
        assertTrue("override fun find(id: Int): String" in content)
        assertTrue("override fun find(name: String): String" in content)
        assertTrue("return findIntBehavior(id)" in content)
        assertTrue("return findStringBehavior(name)" in content)
        assertTrue("fun findInt(behavior: ((Int) -> String))" in content)
        assertTrue("data class OverloadedFindStringCall(val name: String)" in content)
        assertTrue("fun FakeOverloadedImpl.verifyFindInt(" in content)
        assertFalse("findBehavior" in content)
    }

    @Test
    fun `GIVEN class with overloaded open methods WHEN render THEN super calls use source name`() {
        // GIVEN
        val decl =
            classDecl(
                abstractMethods = emptyList(),
                openMethods =
                    listOf(
                        function("count", returnType = "Int"),
                        function("count", param("prefix", "String"), returnType = "Int"),
                    ),
            )

        // WHEN
        val content = FaktCodegen.render(decl).content

        // THEN
        assertTrue("override fun count(prefix: String): Int" in content)
        assertTrue("super.count(prefix)" in content)
        assertTrue("countStringBehavior" in content)
    }

    private fun param(name: String, type: String, isVararg: Boolean = false) =
        ParameterSpec(
            name = name,
            typeString = type,
            hasDefaultValue = false,
            defaultValueCode = null,
            isVararg = isVararg,
        )

    private fun function(
        name: String,
        vararg params: ParameterSpec,
        returnType: String = "Unit",
        receiver: String? = null,
        typeParameters: List<String> = emptyList(),
    ) =
        FunctionSpec(
            name = name,
            parameters = params.toList(),
            returnTypeString = returnType,
            extensionReceiverTypeString = receiver,
            isSuspend = false,
            isInline = false,
            isOperator = false,
            typeParameters = typeParameters,
            typeParameterBounds = emptyMap(),
        )

    private fun interfaceDecl(vararg functions: FunctionSpec, callHistory: Boolean = false) =
        FakeDeclaration.Interface(
            simpleName = "Overloaded",
            qualifiedSourceName = "com.example.Overloaded",
            packageName = "com.example",
            typeParameters = emptyList(),
            visibility = FirVisibility.PUBLIC,
            annotations = emptyList(),
            requiredImports = emptySet(),
            generateCallHistory = callHistory,
            generateMutableBehaviors = false,
            genericPattern = PureGenericPattern.NoGenerics,
            properties = emptyList(),
            functions = functions.toList(),
        )

    private fun classDecl(abstractMethods: List<FunctionSpec>, openMethods: List<FunctionSpec>) =
        FakeDeclaration.Class(
            simpleName = "OverloadedBase",
            qualifiedSourceName = "com.example.OverloadedBase",
            packageName = "com.example",
            typeParameters = emptyList(),
            visibility = FirVisibility.PUBLIC,
            annotations = emptyList(),
            requiredImports = emptySet(),
            generateCallHistory = false,
            generateMutableBehaviors = false,
            constructorParameters = emptyList(),
            abstractMethods = abstractMethods,
            openMethods = openMethods,
            abstractProperties = emptyList(),
            openProperties = emptyList(),
        )
}
