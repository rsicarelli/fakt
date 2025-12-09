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
 * Integration tests for simple interface fake generation.
 *
 * Tests the complete pipeline: Builder → Model → Renderer → Strategy
 * for simple interfaces with basic methods and properties.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimpleInterfaceFakeGenerationTest : IntegrationTestBase() {
    private val resolver = DefaultValueResolver()

    @Test
    fun `GIVEN simple method WHEN generating fake THEN creates behavior property and override`() {
        // GIVEN - Interface: fun getUser(id: String): User?
        val file =
            codeFile("com.example") {
                klass("FakeUserServiceImpl") {
                    implements("UserService")

                    // Behavior property
                    property("getUserBehavior", "(String) -> User?") {
                        private()
                        mutable()
                        initializer = "{ null }"
                    }

                    // Override method
                    function("getUser") {
                        override()
                        parameter("id", "String")
                        returns("User?")
                        body = "return getUserBehavior(id)"
                    }
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        val expected = """
            package com.example

            class FakeUserServiceImpl : UserService {
                private var getUserBehavior: (String) -> User? = { null }

                override fun getUser(id: String): User? {
                    return getUserBehavior(id)
                }
            }
        """

        assertGeneratedCode("Simple method fake", expected, result)
    }

    @Test
    fun `GIVEN property WHEN generating fake THEN creates backing field and override`() {
        // GIVEN - Interface: val userCount: Int
        val file =
            codeFile("com.example") {
                klass("FakeUserServiceImpl") {
                    implements("UserService")

                    // Backing field
                    property("userCountValue", "Int") {
                        private()
                        mutable()
                        initializer = "0"
                    }

                    // Override property
                    property("userCount", "Int") {
                        override()
                        getter = "userCountValue"
                    }
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        val expected = """
            package com.example

            class FakeUserServiceImpl : UserService {
                private var userCountValue: Int = 0

                override val userCount: Int
                    get() = userCountValue
            }
        """

        assertGeneratedCode("Property fake", expected, result)
    }

    @Test
    fun `GIVEN nullable return type WHEN generating default THEN returns null`() {
        // GIVEN - fun getUser(): User?
        val defaultValue = resolver.resolve(parseType("User?"))

        // WHEN
        val rendered = defaultValue.render()

        // THEN
        assertEquals("null", rendered)
    }

    @Test
    fun `GIVEN non-nullable primitive WHEN generating default THEN returns appropriate value`() {
        // GIVEN - val count: Int
        val defaultValue = resolver.resolve(parseType("Int"))

        // WHEN
        val rendered = defaultValue.render()

        // THEN
        assertEquals("0", rendered)
    }

    @Test
    fun `GIVEN complete simple interface WHEN generating fake THEN produces valid class`() {
        // GIVEN - Complete UserService interface
        val file =
            codeFile("com.example.test") {
                import("com.example.User")

                klass("FakeUserServiceImpl") {
                    implements("UserService")

                    // Method: getUser(id: String): User?
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

                    // Property: userCount: Int
                    property("userCountValue", "Int") {
                        private()
                        mutable()
                        initializer = "0"
                    }

                    property("userCount", "Int") {
                        override()
                        getter = "userCountValue"
                    }
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains("Package declaration", "package com.example.test", result)
        assertContains("Import", "import com.example.User", result)
        assertContains("Class declaration", "class FakeUserServiceImpl : UserService", result)
        assertContains("Behavior property", "private var getUserBehavior", result)
        assertContains("Override method", "override fun getUser", result)
        assertContains("Override property", "override val userCount", result)
    }

    @Test
    fun `GIVEN method with multiple parameters WHEN generating THEN preserves all parameters`() {
        // GIVEN - fun create(name: String, age: Int, active: Boolean): User
        val file =
            codeFile("com.example") {
                klass("FakeUserServiceImpl") {
                    function("create") {
                        override()
                        parameter("name", "String")
                        parameter("age", "Int")
                        parameter("active", "Boolean")
                        returns("User")
                        body = "return createBehavior(name, age, active)"
                    }
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains("All parameters", "override fun create(name: String, age: Int, active: Boolean): User", result)
    }

    @Test
    fun `GIVEN method with no parameters WHEN generating THEN creates parameterless function`() {
        // GIVEN - fun getAllUsers(): List<User>
        val file =
            codeFile("com.example") {
                klass("FakeUserServiceImpl") {
                    function("getAllUsers") {
                        override()
                        returns("List<User>")
                        body = "return getAllUsersBehavior()"
                    }
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains("Parameterless function", "override fun getAllUsers(): List<User>", result)
    }

    @Test
    fun `GIVEN String return type WHEN generating default THEN returns empty string`() {
        // GIVEN
        val defaultValue = resolver.resolve(parseType("String"))

        // WHEN
        val rendered = defaultValue.render()

        // THEN
        assertEquals("\"\"", rendered)
    }

    @Test
    fun `GIVEN Boolean return type WHEN generating default THEN returns false`() {
        // GIVEN
        val defaultValue = resolver.resolve(parseType("Boolean"))

        // WHEN
        val rendered = defaultValue.render()

        // THEN
        assertEquals("false", rendered)
    }

    @Test
    fun `GIVEN Unit return type WHEN generating default THEN returns Unit`() {
        // GIVEN
        val defaultValue = resolver.resolve(parseType("Unit"))

        // WHEN
        val rendered = defaultValue.render()

        // THEN
        assertEquals("Unit", rendered)
    }
}
