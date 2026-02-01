// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.builder.codeFile
import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.renderTo
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

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
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "getValue",
                        params = emptyList(),
                        returnType = "String",
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
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "compute",
                        params = listOf(Triple("x", "Int", false), Triple("y", "String", false), Triple("flag", "Boolean", false)),
                        returnType = "Result<Unit>",
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
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "doSomething",
                        params = listOf(Triple("value", "String", false)),
                        returnType = "Unit",
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
        val file =
            codeFile("com.example") {
                klass("FakeAsyncServiceImpl") {
                    overrideMethod(
                        name = "fetchUser",
                        params = listOf(Triple("id", "String", false)),
                        returnType = "User?",
                        config = OverrideMethodConfig(isSuspend = true),
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
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideVarargMethod(
                        name = "process",
                        varargName = "items",
                        varargType = "String",
                        returnType = "Int",
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
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideVarargMethod(
                        name = "log",
                        varargName = "messages",
                        varargType = "String",
                        returnType = "Unit",
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
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    configureMethod(
                        methodName = "getValue",
                        paramTypes = listOf("String"),
                        returnType = "User?",
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "internal fun configureGetValue(behavior: (String) -> User?) = run {")
        assertContains(result, "getValueBehavior = behavior")
    }

    @Test
    fun `GIVEN configureMethod WHEN suspend function THEN generates suspend function type`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeAsyncServiceImpl") {
                    configureMethod(
                        methodName = "saveUser",
                        paramTypes = listOf("User"),
                        returnType = "Result<Unit>",
                        isSuspend = true,
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "internal fun configureSaveUser(behavior: suspend (User) -> Result<Unit>) = run {")
        assertContains(result, "saveUserBehavior = behavior")
    }

    @Test
    fun `GIVEN configureMethod WHEN no parameters THEN generates zero-arg function type`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    configureMethod(
                        methodName = "getData",
                        paramTypes = emptyList(),
                        returnType = "String",
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "internal fun configureGetData(behavior: () -> String) = run {")
    }

    @Test
    fun `GIVEN complete method pattern WHEN using extensions THEN generates full implementation`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeUserServiceImpl") {
                    implements("UserService")

                    // Behavior property
                    behaviorProperty(
                        methodName = "getUser",
                        paramTypes = listOf("String"),
                        returnType = "User?",
                        defaultValue = "{ null }",
                    )

                    // Override method
                    overrideMethod(
                        name = "getUser",
                        params = listOf(Triple("id", "String", false)),
                        returnType = "User?",
                    )

                    // Configure method
                    configureMethod(
                        methodName = "getUser",
                        paramTypes = listOf("String"),
                        returnType = "User?",
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
        assertContains(result, "internal fun configureGetUser(behavior: (String) -> User?) = run {")
        assertContains(result, "getUserBehavior = behavior")
    }

    // region Call History Update Tests

    @Test
    fun `GIVEN overrideMethod WHEN 0-param THEN includes Unit history update`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "logout",
                        params = emptyList(),
                        returnType = "Unit",
                        config = OverrideMethodConfig(interfaceName = "AuthService"),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Only history update (call count is derived from history size)
        assertContains(result, "_logoutCalls.update { it + Unit }")
    }

    @Test
    fun `GIVEN overrideMethod WHEN with params THEN includes data class history update`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "getUser",
                        params = listOf(Triple("id", "String", false)),
                        returnType = "User",
                        config = OverrideMethodConfig(interfaceName = "UserService"),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Only history update (call count is derived from history size)
        assertContains(result, "_getUserCalls.update { it + UserServiceGetUserCall(id) }")
    }

    @Test
    fun `GIVEN overrideMethod WHEN multiple params THEN includes all params in history`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "createOrder",
                        params =
                            listOf(
                                Triple("userId", "String", false),
                                Triple("amount", "Int", false),
                            ),
                        returnType = "Order",
                        config = OverrideMethodConfig(interfaceName = "OrderService"),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "_createOrderCalls.update { it + OrderServiceCreateOrderCall(userId, amount) }")
    }

    @Test
    fun `GIVEN overrideMethod WHEN vararg param only THEN uses Unit history`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "log",
                        params = listOf(Triple("messages", "Array<out String>", true)),
                        returnType = "Unit",
                        config = OverrideMethodConfig(interfaceName = "LogService"),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "_logCalls.update { it + Unit }")
    }

    @Test
    fun `GIVEN overrideMethod WHEN mixed params with vararg THEN excludes vararg from history`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "format",
                        params =
                            listOf(
                                Triple("template", "String", false),
                                Triple("args", "Array<out Any>", true),
                            ),
                        returnType = "String",
                        config = OverrideMethodConfig(interfaceName = "FormatService"),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "_formatCalls.update { it + FormatServiceFormatCall(template) }")
        // Should NOT include args in the data class
        assertFalse(result.contains("FormatServiceFormatCall(template, args)"))
    }

    @Test
    fun `GIVEN overrideVarargMethod WHEN called THEN includes Unit history update`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideVarargMethod(
                        name = "process",
                        varargName = "items",
                        varargType = "String",
                        returnType = "Int",
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Only history update (call count is derived from history size)
        assertContains(result, "_processCalls.update { it + Unit }")
    }

    @Test
    fun `GIVEN overrideMethod WHEN generic param THEN casts to erased type in history`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeRepositoryImpl") {
                    overrideMethod(
                        name = "save",
                        params = listOf(Triple("item", "T", false)),
                        returnType = "Unit",
                        config =
                            OverrideMethodConfig(
                                typeParameters = listOf("T"),
                                interfaceName = "Repository",
                            ),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "_saveCalls.update { it + RepositorySaveCall(item as Any?) }")
    }

    // endregion

    // region generateCallHistory Control Tests

    @Test
    fun `GIVEN overrideMethod with generateCallHistory false WHEN generating THEN omits history update`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "getUser",
                        params = listOf(Triple("id", "String", false)),
                        returnType = "User",
                        config =
                            OverrideMethodConfig(
                                interfaceName = "UserService",
                                generateCallHistory = false,
                            ),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should NOT contain history update
        assertFalse(
            result.contains("_getUserCalls.update"),
            "Should not contain history update when generateCallHistory is false",
        )
        // But SHOULD still contain the behavior call
        assertContains(result, "return getUserBehavior(id)")
    }

    @Test
    fun `GIVEN overrideMethod with generateCallHistory false WHEN 0-param THEN omits Unit history update`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "logout",
                        params = emptyList(),
                        returnType = "Unit",
                        config =
                            OverrideMethodConfig(
                                interfaceName = "AuthService",
                                generateCallHistory = false,
                            ),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should NOT contain any history update
        assertFalse(
            result.contains("_logoutCalls.update"),
            "Should not contain history update when generateCallHistory is false",
        )
        // But SHOULD still contain the behavior call
        assertContains(result, "logoutBehavior()")
    }

    @Test
    fun `GIVEN overrideVarargMethod with generateCallHistory false WHEN generating THEN omits history update`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideVarargMethod(
                        name = "process",
                        varargName = "items",
                        varargType = "String",
                        returnType = "Int",
                        config = OverrideVarargConfig(generateCallHistory = false),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should NOT contain history update
        assertFalse(
            result.contains("_processCalls.update"),
            "Should not contain history update when generateCallHistory is false",
        )
        // But SHOULD still contain the behavior call
        assertContains(result, "return processBehavior(items)")
    }

    @Test
    fun `GIVEN overrideMethod with generateCallHistory true WHEN generating THEN includes history update`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "doWork",
                        params = listOf(Triple("value", "Int", false)),
                        returnType = "String",
                        config =
                            OverrideMethodConfig(
                                interfaceName = "WorkService",
                                generateCallHistory = true, // explicitly enabled
                            ),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should contain history update
        assertContains(result, "_doWorkCalls.update { it + WorkServiceDoWorkCall(value) }")
    }

    @Test
    fun `GIVEN overrideMethod with default config WHEN generating THEN includes history update`() {
        // GIVEN - Using default OverrideMethodConfig which has generateCallHistory = true
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideMethod(
                        name = "getData",
                        params = emptyList(),
                        returnType = "String",
                        config = OverrideMethodConfig(interfaceName = "DataService"),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should contain history update (default behavior)
        assertContains(result, "_getDataCalls.update { it + Unit }")
    }

    @Test
    fun `GIVEN overrideVarargMethod with generateCallHistory true WHEN generating THEN includes history update`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeServiceImpl") {
                    overrideVarargMethod(
                        name = "log",
                        varargName = "messages",
                        varargType = "String",
                        returnType = "Unit",
                        config = OverrideVarargConfig(generateCallHistory = true),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should contain history update
        assertContains(result, "_logCalls.update { it + Unit }")
    }

    @Test
    fun `GIVEN suspend method with generateCallHistory false WHEN generating THEN omits history update`() {
        // GIVEN
        val file =
            codeFile("com.example") {
                klass("FakeAsyncServiceImpl") {
                    overrideMethod(
                        name = "fetchUser",
                        params = listOf(Triple("id", "String", false)),
                        returnType = "User?",
                        config =
                            OverrideMethodConfig(
                                isSuspend = true,
                                interfaceName = "AsyncService",
                                generateCallHistory = false,
                            ),
                    )
                }
            }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should NOT contain history update
        assertFalse(
            result.contains("_fetchUserCalls.update"),
            "Suspend method should not have history update when disabled",
        )
        // But SHOULD still be a suspend function with correct behavior
        assertContains(result, "override suspend fun fetchUser")
        assertContains(result, "return fetchUserBehavior(id)")
    }

    // endregion
}
