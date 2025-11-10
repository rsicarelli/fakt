// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.renderTo
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertContains

/**
 * Tests for method extension functions.
 *
 * Validates method generation patterns for overrides, vararg, suspend, and configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MethodExtensionsTest {

    @Test
    fun `GIVEN overrideMethod WHEN no parameters THEN generates zero-arg override`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                overrideMethod(
                    name = "getValue",
                    params = emptyList(),
                    returnType = "String"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override fun getValue(): String {")
        assertContains(result, "return getValueBehavior()")
    }

    @Test
    fun `GIVEN overrideMethod WHEN multiple parameters THEN generates multi-param override`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                overrideMethod(
                    name = "compute",
                    params = listOf("x" to "Int", "y" to "String", "flag" to "Boolean"),
                    returnType = "Result<Unit>"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override fun compute(x: Int, y: String, flag: Boolean): Result<Unit> {")
        assertContains(result, "return computeBehavior(x, y, flag)")
    }

    @Test
    fun `GIVEN overrideMethod WHEN Unit return type THEN omits return keyword`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                overrideMethod(
                    name = "doSomething",
                    params = listOf("value" to "String"),
                    returnType = "Unit"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override fun doSomething(value: String): Unit {")
        assertContains(result, "doSomethingBehavior(value)")
        // Should NOT contain "return doSomethingBehavior"
    }

    @Test
    fun `GIVEN overrideMethod WHEN isSuspend true THEN generates suspend function`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeAsyncServiceImpl") {
                overrideMethod(
                    name = "fetchUser",
                    params = listOf("id" to "String"),
                    returnType = "User?",
                    isSuspend = true
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override suspend fun fetchUser(id: String): User? {")
        assertContains(result, "return fetchUserBehavior(id)")
    }

    @Test
    fun `GIVEN overrideVarargMethod WHEN generating THEN creates vararg parameter`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                overrideVarargMethod(
                    name = "process",
                    varargName = "items",
                    varargType = "String",
                    returnType = "Int"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override fun process(vararg items: String): Int {")
        assertContains(result, "return processBehavior(items)")
    }

    @Test
    fun `GIVEN overrideVarargMethod WHEN Unit return THEN omits return keyword`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                overrideVarargMethod(
                    name = "log",
                    varargName = "messages",
                    varargType = "String",
                    returnType = "Unit"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override fun log(vararg messages: String): Unit {")
        assertContains(result, "logBehavior(messages)")
    }

    @Test
    fun `GIVEN configureMethod WHEN regular function THEN generates internal configure method`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                configureMethod(
                    methodName = "getValue",
                    paramTypes = listOf("String"),
                    returnType = "User?"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "internal fun configureGetValue(behavior: (String) -> User?): Unit {")
        assertContains(result, "getValueBehavior = behavior")
    }

    @Test
    fun `GIVEN configureMethod WHEN suspend function THEN generates suspend function type`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeAsyncServiceImpl") {
                configureMethod(
                    methodName = "saveUser",
                    paramTypes = listOf("User"),
                    returnType = "Result<Unit>",
                    isSuspend = true
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "internal fun configureSaveUser(behavior: suspend (User) -> Result<Unit>): Unit {")
        assertContains(result, "saveUserBehavior = behavior")
    }

    @Test
    fun `GIVEN configureMethod WHEN no parameters THEN generates zero-arg function type`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                configureMethod(
                    methodName = "getData",
                    paramTypes = emptyList(),
                    returnType = "String"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "internal fun configureGetData(behavior: () -> String): Unit {")
    }

    @Test
    fun `GIVEN delegateToBehavior extension WHEN applied THEN generates delegation body`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                function("getUser") {
                    override()
                    parameter("id", "String")
                    returns("User?")
                    delegateToBehavior("getUserBehavior", listOf("id"))
                }
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override fun getUser(id: String): User? {")
        assertContains(result, "return getUserBehavior(id)")
    }

    @Test
    fun `GIVEN asSimpleOverride extension WHEN applied THEN generates inline body`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                function("getValue") {
                    returns("String")
                    asSimpleOverride("return \"test\"")
                }
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override fun getValue(): String {")
        assertContains(result, "return \"test\"")
    }

    @Test
    fun `GIVEN overridePropertyGetter WHEN generating THEN creates property with getter`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                overridePropertyGetter(
                    name = "users",
                    type = "StateFlow<List<User>>",
                    backingPropertyName = "usersValue"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override val users: StateFlow<List<User>>")
        assertContains(result, "get() = usersValue")
    }

    @Test
    fun `GIVEN complete method pattern WHEN using extensions THEN generates full implementation`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeUserServiceImpl") {
                implements("UserService")

                // Behavior property
                behaviorProperty(
                    methodName = "getUser",
                    paramTypes = listOf("String"),
                    returnType = "User?",
                    defaultValue = "{ null }"
                )

                // Override method
                overrideMethod(
                    name = "getUser",
                    params = listOf("id" to "String"),
                    returnType = "User?"
                )

                // Configure method
                configureMethod(
                    methodName = "getUser",
                    paramTypes = listOf("String"),
                    returnType = "User?"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "class FakeUserServiceImpl : UserService")
        assertContains(result, "private var getUserBehavior: (String) -> User? = { null }")
        assertContains(result, "override fun getUser(id: String): User? {")
        assertContains(result, "return getUserBehavior(id)")
        assertContains(result, "internal fun configureGetUser(behavior: (String) -> User?): Unit {")
        assertContains(result, "getUserBehavior = behavior")
    }
}
