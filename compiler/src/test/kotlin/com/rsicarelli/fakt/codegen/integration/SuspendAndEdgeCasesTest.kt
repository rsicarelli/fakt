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

/**
 * Integration tests for suspend functions and edge cases.
 *
 * Covers:
 * - Suspend function generation
 * - Edge cases and formatting
 * - Complete file generation
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SuspendAndEdgeCasesTest : IntegrationTestBase() {

    private val resolver = DefaultValueResolver()

    // ===========================================
    // Suspend Functions
    // ===========================================

    @Test
    fun `GIVEN suspend function WHEN generating THEN includes suspend modifier`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeAsyncServiceImpl") {
                implements("AsyncService")

                function("fetchUser") {
                    override()
                    suspend()
                    parameter("id", "String")
                    returns("User?")
                    body = "return fetchUserBehavior(id)"
                }
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains("Suspend modifier", "override suspend fun fetchUser", result)
    }

    @Test
    fun `GIVEN suspend function with Result WHEN generating THEN combines suspend and Result`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeAsyncServiceImpl") {
                function("saveUser") {
                    override()
                    suspend()
                    parameter("user", "User")
                    returns("Result<Unit>")
                    body = "return saveUserBehavior(user)"
                }
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains("Suspend with Result", "override suspend fun saveUser(user: User): Result<Unit>", result)
    }

    // ===========================================
    // Edge Cases
    // ===========================================

    @Test
    fun `GIVEN empty interface WHEN generating THEN creates minimal fake`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeEmptyServiceImpl") {
                implements("EmptyService")
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        val expected = """
            package com.example

            class FakeEmptyServiceImpl : EmptyService {
            }
        """

        assertGeneratedCode("Empty interface", expected, result)
    }

    @Test
    fun `GIVEN deeply nested generics WHEN rendering THEN formats correctly`() {
        // GIVEN
        val nestedType = parseType("Map<String, List<Map<Int, User>>>")

        // WHEN
        val defaultValue = resolver.resolve(nestedType)
        val rendered = defaultValue.render()

        // THEN
        // Should be emptyMap() since it's a Map
        assertContains("Nested generics handled", "emptyMap", rendered)
    }

    @Test
    fun `GIVEN interface with multiple methods WHEN generating THEN separates with blank lines`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                implements("Service")

                property("method1Behavior", "() -> String") {
                    private()
                    mutable()
                    initializer = "{ \"\" }"
                }

                function("method1") {
                    override()
                    returns("String")
                    body = "return method1Behavior()"
                }

                property("method2Behavior", "() -> Int") {
                    private()
                    mutable()
                    initializer = "{ 0 }"
                }

                function("method2") {
                    override()
                    returns("Int")
                    body = "return method2Behavior()"
                }
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains("Multiple members", "private var method1Behavior", result)
        assertContains("Multiple members", "override fun method1", result)
        assertContains("Multiple members", "private var method2Behavior", result)
        assertContains("Multiple members", "override fun method2", result)
    }

    // ===========================================
    // Complete File Generation
    // ===========================================

    @Test
    fun `GIVEN complete interface WHEN generating full file THEN includes all sections`() {
        // GIVEN
        val file = codeFile("com.example.test") {
            header = "Generated by Fakt"

            import("com.example.User")
            import("kotlinx.coroutines.flow.StateFlow")
            import("kotlinx.coroutines.flow.MutableStateFlow")

            klass("FakeUserServiceImpl") {
                implements("UserService")

                // Simple method
                property("getUserBehavior", "(String) -> User?") {
                    private()
                    mutable()
                    initializer = "{ null }"
                }

                function("getUser") {
                    override()
                    parameter("id", "String")
                    returns("User?")
                    body = "return getUserBehavior(id)"
                }

                // Collection return
                property("getAllUsersBehavior", "() -> List<User>") {
                    private()
                    mutable()
                    initializer = "{ emptyList() }"
                }

                function("getAllUsers") {
                    override()
                    returns("List<User>")
                    body = "return getAllUsersBehavior()"
                }

                // StateFlow property
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

        // THEN - Complete file structure
        assertContains("Header", "// Generated by Fakt", result)
        assertContains("Package", "package com.example.test", result)
        assertContains("Import User", "import com.example.User", result)
        assertContains("Import StateFlow", "import kotlinx.coroutines.flow.StateFlow", result)
        assertContains("Import MutableStateFlow", "import kotlinx.coroutines.flow.MutableStateFlow", result)
        assertContains("Class declaration", "class FakeUserServiceImpl : UserService", result)
        assertContains("Simple method behavior", "private var getUserBehavior", result)
        assertContains("Collection method behavior", "private var getAllUsersBehavior", result)
        assertContains("StateFlow property", "private val usersValue", result)
        assertContains("Closing brace", "}", result)
    }

    @Test
    fun `GIVEN package name with keywords WHEN generating THEN escapes correctly`() {
        // GIVEN
        val file = codeFile("com.example.`class`.test") {
            klass("FakeServiceImpl") {
                implements("Service")
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        // Package name should have keyword escaped
        assertContains("Package with keyword", "package com.example.`class`.test", result)
    }

    @Test
    fun `GIVEN function with default parameters WHEN generating THEN includes defaults`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                function("process") {
                    override()
                    parameter("value", "String", defaultValue = "\"\"")
                    parameter("count", "Int", defaultValue = "0")
                    returns("Unit")
                    body = "return processBehavior(value, count)"
                }
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains("Default parameter 1", "value: String = \"\"", result)
        assertContains("Default parameter 2", "count: Int = 0", result)
    }

    @Test
    fun `GIVEN custom indentation size WHEN building THEN respects indent`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                implements("Service")

                property("valueBehavior", "() -> Int") {
                    private()
                    mutable()
                    initializer = "{ 0 }"
                }
            }
        }

        // WHEN - Use custom indent size
        val builder = CodeBuilder(indentSize = 2)
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should have 2-space indentation
        assertContains("2-space indent", "  private var valueBehavior", result)
    }

    @Test
    fun `GIVEN MutableList return type WHEN generating default THEN returns mutableListOf`() {
        // GIVEN
        val mutableListType = parseType("MutableList<User>")

        // WHEN
        val defaultValue = resolver.resolve(mutableListType)
        val rendered = defaultValue.render()

        // THEN
        assertContains("MutableList default", "mutableListOf", rendered)
    }
}
