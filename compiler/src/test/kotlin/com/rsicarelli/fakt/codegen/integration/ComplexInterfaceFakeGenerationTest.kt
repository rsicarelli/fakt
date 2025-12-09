// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.integration

import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.builder.parseType
import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.render
import com.rsicarelli.fakt.codegen.renderer.renderTo
import com.rsicarelli.fakt.codegen.strategy.DefaultValueResolver
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for complex interface fake generation.
 *
 * Tests collections, generic types, nested types, and composition.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ComplexInterfaceFakeGenerationTest : IntegrationTestBase() {
    private val resolver = DefaultValueResolver()

    @Test
    fun `GIVEN List return type WHEN generating default THEN returns emptyList`() {
        // GIVEN
        val listType = parseType("List<User>")

        // WHEN
        val defaultValue = resolver.resolve(listType)
        val rendered = defaultValue.render()

        // THEN
        assertEquals("emptyList()", rendered)
    }

    @Test
    fun `GIVEN Set return type WHEN generating default THEN returns emptySet`() {
        // GIVEN
        val setType = parseType("Set<Int>")

        // WHEN
        val defaultValue = resolver.resolve(setType)

        // THEN
        assertEquals("emptySet()", defaultValue.render())
    }

    @Test
    fun `GIVEN Map return type WHEN generating default THEN returns emptyMap`() {
        // GIVEN
        val mapType = parseType("Map<String, User>")

        // WHEN
        val defaultValue = resolver.resolve(mapType)

        // THEN
        assertEquals("emptyMap()", defaultValue.render())
    }

    @Test
    fun `GIVEN StateFlow return type WHEN generating default THEN returns MutableStateFlow with nested default`() {
        // GIVEN
        val stateFlowType = parseType("StateFlow<Int>")

        // WHEN
        val defaultValue = resolver.resolve(stateFlowType)
        val rendered = defaultValue.render()

        // THEN
        assertEquals("MutableStateFlow(0)", rendered)
    }

    @Test
    fun `GIVEN Result return type WHEN generating default THEN returns Result success with nested default`() {
        // GIVEN
        val resultType = parseType("Result<String>")

        // WHEN
        val defaultValue = resolver.resolve(resultType)
        val rendered = defaultValue.render()

        // THEN
        assertEquals("Result.success(\"\")", rendered)
    }

    @Test
    fun `GIVEN Flow return type WHEN generating default THEN returns emptyFlow`() {
        // GIVEN
        val flowType = parseType("Flow<User>")

        // WHEN
        val defaultValue = resolver.resolve(flowType)

        // THEN
        assertEquals("emptyFlow()", defaultValue.render())
    }

    @Test
    fun `GIVEN StateFlow of List WHEN generating default THEN composes correctly`() {
        // GIVEN
        val nestedType = parseType("StateFlow<List<User>>")

        // WHEN
        val defaultValue = resolver.resolve(nestedType)
        val rendered = defaultValue.render()

        // THEN
        assertEquals("MutableStateFlow(emptyList())", rendered)
    }

    @Test
    fun `GIVEN Result of StateFlow WHEN generating default THEN composes correctly`() {
        // GIVEN
        val nestedType = parseType("Result<StateFlow<Int>>")

        // WHEN
        val defaultValue = resolver.resolve(nestedType)
        val rendered = defaultValue.render()

        // THEN
        assertEquals("Result.success(MutableStateFlow(0))", rendered)
    }

    @Test
    fun `GIVEN complex repository interface WHEN generating fake THEN produces complete implementation`() {
        // GIVEN - Repository with collections and complex types
        val file =
            codeFile("com.example") {
                import("com.example.User")
                import("kotlinx.coroutines.flow.StateFlow")
                import("kotlinx.coroutines.flow.MutableStateFlow")

                klass("FakeRepositoryImpl") {
                    implements("Repository")

                    // Method: fun findAll(): List<User>
                    property("findAllBehavior", "() -> List<User>") {
                        private()
                        mutable()
                        initializer = "{ emptyList() }"
                    }

                    function("findAll") {
                        override()
                        returns("List<User>")
                        body = "return findAllBehavior()"
                    }

                    // Property: val users: StateFlow<List<User>>
                    property("usersValue", "StateFlow<List<User>>") {
                        private()
                        initializer = "MutableStateFlow(emptyList())"
                    }

                    property("users", "StateFlow<List<User>>") {
                        override()
                        getter = "usersValue"
                    }
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains("Package", "package com.example", result)
        assertContains("Imports", "import com.example.User", result)
        assertContains("StateFlow import", "import kotlinx.coroutines.flow.StateFlow", result)
        assertContains(
            "Behavior property",
            "private var findAllBehavior: () -> List<User> = { emptyList() }",
            result,
        )
        assertContains("Override method", "override fun findAll(): List<User>", result)
        assertContains(
            "StateFlow property",
            "private val usersValue: StateFlow<List<User>> = MutableStateFlow(emptyList())",
            result,
        )
        assertContains(
            "Override property with getter",
            "override val users: StateFlow<List<User>>",
            result,
        )
    }

    @Test
    fun `GIVEN nullable List WHEN generating default THEN returns null`() {
        // GIVEN
        val nullableListType = parseType("List<User>?")

        // WHEN
        val defaultValue = resolver.resolve(nullableListType)

        // THEN
        assertEquals("null", defaultValue.render())
    }

    @Test
    fun `GIVEN List of nullable WHEN generating default THEN returns emptyList`() {
        // GIVEN
        val listOfNullableType = parseType("List<User?>")

        // WHEN
        val defaultValue = resolver.resolve(listOfNullableType)

        // THEN
        // List<User?> is still a List, so emptyList() is correct
        assertEquals("emptyList()", defaultValue.render())
    }

    @Test
    fun `GIVEN Map with complex value type WHEN rendering THEN preserves all type arguments`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeCacheImpl") {
                    function("get") {
                        override()
                        parameter("key", "String")
                        returns("Map<String, List<User>>")
                        body = "return getBehavior(key)"
                    }
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains("Map with nested List", "Map<String, List<User>>", result)
    }
}
