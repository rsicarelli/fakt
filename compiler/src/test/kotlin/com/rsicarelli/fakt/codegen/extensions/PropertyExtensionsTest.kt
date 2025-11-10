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
 * Tests for property extension functions.
 *
 * Validates property generation patterns for StateFlow, behaviors, and common patterns.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PropertyExtensionsTest {

    @Test
    fun `GIVEN stateFlowProperty WHEN generating THEN creates backing MutableStateFlow and override getter`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeUserStoreImpl") {
                implements("UserStore")
                stateFlowProperty(
                    name = "users",
                    elementType = "List<User>",
                    defaultValue = "emptyList()"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "private val usersValue: StateFlow<List<User>> = MutableStateFlow(emptyList())")
        assertContains(result, "override val users: StateFlow<List<User>>")
        assertContains(result, "get() = usersValue")
    }

    @Test
    fun `GIVEN behaviorProperty WHEN no parameters THEN creates zero-arg function type`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                behaviorProperty(
                    methodName = "getValue",
                    paramTypes = emptyList(),
                    returnType = "String",
                    defaultValue = "{ \"\" }"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "private var getValueBehavior: () -> String = { \"\" }")
    }

    @Test
    fun `GIVEN behaviorProperty WHEN multiple parameters THEN creates multi-arg function type`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                behaviorProperty(
                    methodName = "compute",
                    paramTypes = listOf("Int", "String", "Boolean"),
                    returnType = "Result<Unit>",
                    defaultValue = "{ _, _, _ -> Result.success(Unit) }"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "private var computeBehavior: (Int, String, Boolean) -> Result<Unit> =")
    }

    @Test
    fun `GIVEN suspendBehaviorProperty WHEN no parameters THEN creates suspend function type`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeAsyncServiceImpl") {
                suspendBehaviorProperty(
                    methodName = "fetchData",
                    paramTypes = emptyList(),
                    returnType = "Data?",
                    defaultValue = "{ null }"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "private var fetchDataBehavior: suspend () -> Data? = { null }")
    }

    @Test
    fun `GIVEN suspendBehaviorProperty WHEN with parameters THEN creates suspend multi-arg function type`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeAsyncServiceImpl") {
                suspendBehaviorProperty(
                    methodName = "saveUser",
                    paramTypes = listOf("User", "Boolean"),
                    returnType = "Result<Unit>",
                    defaultValue = "{ _, _ -> Result.success(Unit) }"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "private var saveUserBehavior: suspend (User, Boolean) -> Result<Unit> =")
    }

    @Test
    fun `GIVEN mutableProperty WHEN generating THEN creates private var with initializer`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeCounterImpl") {
                mutableProperty(
                    name = "count",
                    type = "Int",
                    defaultValue = "0"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "private var count: Int = 0")
    }

    @Test
    fun `GIVEN asBehavior extension WHEN applied THEN configures as private mutable`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                property("testBehavior", "() -> Unit") {
                    asBehavior()
                    initializer = "{}"
                }
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "private var testBehavior: () -> Unit = {}")
    }

    @Test
    fun `GIVEN asStateFlowBacking extension WHEN applied THEN configures as private val`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                property("stateValue", "StateFlow<Int>") {
                    asStateFlowBacking()
                    initializer = "MutableStateFlow(0)"
                }
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "private val stateValue: StateFlow<Int> = MutableStateFlow(0)")
    }

    @Test
    fun `GIVEN asOverrideWithGetter extension WHEN applied THEN configures as override with getter`() {
        // GIVEN
        val file = codeFile("com.example") {
            klass("FakeServiceImpl") {
                property("state", "StateFlow<Int>") {
                    asOverrideWithGetter("stateValue")
                }
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override val state: StateFlow<Int>")
        assertContains(result, "get() = stateValue")
    }

    @Test
    fun `GIVEN complete StateFlow pattern WHEN using stateFlowProperty THEN generates full implementation`() {
        // GIVEN
        val file = codeFile("com.example.test") {
            import("kotlinx.coroutines.flow.StateFlow")
            import("kotlinx.coroutines.flow.MutableStateFlow")

            klass("FakeUserStoreImpl") {
                implements("UserStore")

                stateFlowProperty(
                    name = "currentUser",
                    elementType = "User?",
                    defaultValue = "null"
                )
            }
        }

        // WHEN
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "package com.example.test")
        assertContains(result, "import kotlinx.coroutines.flow.StateFlow")
        assertContains(result, "import kotlinx.coroutines.flow.MutableStateFlow")
        assertContains(result, "class FakeUserStoreImpl : UserStore")
        assertContains(result, "private val currentUserValue: StateFlow<User?> = MutableStateFlow(null)")
        assertContains(result, "override val currentUser: StateFlow<User?>")
        assertContains(result, "get() = currentUserValue")
    }
}
