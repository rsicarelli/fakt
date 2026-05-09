// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen

import com.rsicarelli.fakt.codegen.analysis.ConstructorParam
import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.codegen.analysis.FunctionSpec
import com.rsicarelli.fakt.codegen.analysis.ParameterSpec
import com.rsicarelli.fakt.codegen.analysis.PropertySpec
import com.rsicarelli.fakt.codegen.analysis.PureGenericPattern
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Verifies the [FaktCodegen] façade can render fakes from hand-built declarations — proving the
 * library is callable with no compiler instance, no IrType resolution, and no Gradle plugin.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktCodegenTest {
    @Test
    fun `GIVEN interface declaration WHEN render THEN file contains fake impl factory and config`() {
        // GIVEN
        val decl =
            FakeDeclaration.Interface(
                simpleName = "UserRepository",
                qualifiedSourceName = "com.example.auth.UserRepository",
                packageName = "com.example.auth",
                typeParameters = emptyList(),
                visibility = FirVisibility.PUBLIC,
                annotations = emptyList(),
                requiredImports = emptySet(),
                generateCallHistory = false,
                generateMutableBehaviors = false,
                genericPattern = PureGenericPattern.NoGenerics,
                properties =
                    listOf(
                        PropertySpec(
                            name = "currentUser",
                            typeString = "String",
                            isMutable = false,
                            isNullable = false,
                        )
                    ),
                functions =
                    listOf(
                        FunctionSpec(
                            name = "logout",
                            parameters = emptyList(),
                            returnTypeString = "Unit",
                            extensionReceiverTypeString = null,
                            isSuspend = false,
                            isInline = false,
                            isOperator = false,
                            typeParameters = emptyList(),
                            typeParameterBounds = emptyMap(),
                        )
                    ),
            )

        // WHEN
        val rendered = FaktCodegen.render(decl)

        // THEN
        assertEquals("com.example.auth", rendered.packageName)
        assertEquals("FakeUserRepositoryImpl.kt", rendered.fileName)
        assertTrue("package com.example.auth" in rendered.content, "must declare target package")
        assertTrue("class FakeUserRepositoryImpl" in rendered.content, "must declare fake impl")
        assertTrue("fun fakeUserRepository" in rendered.content, "must declare factory function")
        assertTrue("class FakeUserRepositoryConfig" in rendered.content, "must declare config DSL")
    }

    @Test
    fun `GIVEN class declaration WHEN render THEN file forwards constructor params`() {
        // GIVEN
        val decl =
            FakeDeclaration.Class(
                simpleName = "BaseService",
                qualifiedSourceName = "com.example.BaseService",
                packageName = "com.example",
                typeParameters = emptyList(),
                visibility = FirVisibility.PUBLIC,
                annotations = emptyList(),
                requiredImports = emptySet(),
                generateCallHistory = false,
                generateMutableBehaviors = false,
                constructorParameters =
                    listOf(
                        ConstructorParam(name = "label", typeString = "String", hasDefault = false)
                    ),
                abstractMethods =
                    listOf(
                        FunctionSpec(
                            name = "compute",
                            parameters =
                                listOf(
                                    ParameterSpec(
                                        name = "input",
                                        typeString = "Int",
                                        hasDefaultValue = false,
                                        defaultValueCode = null,
                                        isVararg = false,
                                    )
                                ),
                            returnTypeString = "Int",
                            extensionReceiverTypeString = null,
                            isSuspend = false,
                            isInline = false,
                            isOperator = false,
                            typeParameters = emptyList(),
                            typeParameterBounds = emptyMap(),
                        )
                    ),
                openMethods = emptyList(),
                abstractProperties = emptyList(),
                openProperties = emptyList(),
            )

        // WHEN
        val rendered = FaktCodegen.render(decl)

        // THEN
        assertEquals("FakeBaseServiceImpl.kt", rendered.fileName)
        assertTrue("class FakeBaseServiceImpl" in rendered.content, "must declare fake impl class")
        assertTrue("label = " in rendered.content, "must forward constructor param")
    }

    @Test
    fun `GIVEN imports WHEN render THEN file emits import statements`() {
        // GIVEN
        val decl =
            FakeDeclaration.Interface(
                simpleName = "Service",
                qualifiedSourceName = "com.example.Service",
                packageName = "com.example",
                typeParameters = emptyList(),
                visibility = FirVisibility.PUBLIC,
                annotations = emptyList(),
                requiredImports = emptySet(),
                generateCallHistory = false,
                generateMutableBehaviors = false,
                genericPattern = PureGenericPattern.NoGenerics,
                properties = emptyList(),
                functions = emptyList(),
            )

        // WHEN
        val rendered = FaktCodegen.render(decl, imports = listOf("kotlinx.coroutines.flow.Flow"))

        // THEN
        assertTrue(
            "import kotlinx.coroutines.flow.Flow" in rendered.content,
            "caller-provided imports should appear in the rendered file",
        )
    }
}
