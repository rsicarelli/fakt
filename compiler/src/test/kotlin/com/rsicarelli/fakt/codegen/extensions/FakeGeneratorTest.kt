// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.renderTo
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertContains

/**
 * Tests for high-level fake generator function.
 *
 * Validates complete fake generation with methods, properties, and configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeGeneratorTest {
    @Test
    fun `GIVEN generateCompleteFake WHEN simple method THEN generates complete implementation`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(
                    name = "getUser",
                    params = listOf(Triple("id", "String", false)),
                    returnType = "User?",
                ),
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "UserService",
                methods = methods,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "package com.example")
        assertContains(result, "class FakeUserServiceImpl : UserService")
        assertContains(result, "private var getUserBehavior: (String) -> User? = { null }")
        assertContains(result, "override fun getUser(id: String): User? {")
        assertContains(result, "return getUserBehavior(id)")
        assertContains(result, "internal fun configureGetUser(behavior: (String) -> User?): Unit {")
    }

    @Test
    fun `GIVEN generateCompleteFake WHEN suspend method THEN generates suspend implementation`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(
                    name = "saveUser",
                    params = listOf(Triple("user", "User", false)),
                    returnType = "Result<Unit>",
                    isSuspend = true,
                ),
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "UserService",
                methods = methods,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(
            result,
            "private var saveUserBehavior: suspend (User) -> Result<Unit> = { Result.success(Unit) }",
        )
        assertContains(result, "override suspend fun saveUser(user: User): Result<Unit> {")
        assertContains(result, "return saveUserBehavior(user)")
        assertContains(
            result,
            "internal fun configureSaveUser(behavior: suspend (User) -> Result<Unit>): Unit {",
        )
    }

    @Test
    fun `GIVEN generateCompleteFake WHEN vararg method THEN generates vararg implementation`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(
                    name = "process",
                    params = listOf(Triple("items", "String", false)),
                    returnType = "Int",
                    isVararg = true,
                ),
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "Service",
                methods = methods,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "override fun process(vararg items: String): Int {")
        assertContains(result, "return processBehavior(items)")
    }

    @Test
    fun `GIVEN generateCompleteFake WHEN StateFlow property THEN generates StateFlow with backing`() {
        // GIVEN
        val properties =
            listOf(
                PropertySpec(
                    name = "users",
                    type = "StateFlow<List<User>>",
                    isStateFlow = true,
                ),
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "UserStore",
                properties = properties,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "import kotlinx.coroutines.flow.StateFlow")
        assertContains(result, "import kotlinx.coroutines.flow.MutableStateFlow")
        assertContains(
            result,
            "private val usersValue: StateFlow<List<User>> = MutableStateFlow(emptyList())",
        )
        assertContains(result, "override val users: StateFlow<List<User>>")
        assertContains(result, "get() = usersValue")
    }

    @Test
    fun `GIVEN generateCompleteFake WHEN simple property THEN generates property with backing`() {
        // GIVEN
        val properties =
            listOf(
                PropertySpec(
                    name = "count",
                    type = "Int",
                    isStateFlow = false,
                ),
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "Counter",
                properties = properties,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "private val countValue: Int = 0")
        assertContains(result, "override val count: Int")
        assertContains(result, "get() = countValue")
    }

    @Test
    fun `GIVEN generateCompleteFake WHEN multiple methods THEN generates all implementations`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec("getUser", listOf(Triple("id", "String", false)), "User?"),
                MethodSpec(
                    "saveUser",
                    listOf(Triple("user", "User", false)),
                    "Result<Unit>",
                    isSuspend = true,
                ),
                MethodSpec("deleteUser", listOf(Triple("id", "String", false)), "Unit"),
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "UserService",
                methods = methods,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "private var getUserBehavior")
        assertContains(result, "override fun getUser(id: String): User?")
        assertContains(result, "private var saveUserBehavior: suspend")
        assertContains(result, "override suspend fun saveUser(user: User): Result<Unit>")
        assertContains(result, "private var deleteUserBehavior")
        assertContains(result, "override fun deleteUser(id: String): Unit")
    }

    @Test
    fun `GIVEN generateCompleteFake WHEN custom imports THEN includes all imports`() {
        // GIVEN
        val imports =
            listOf(
                "com.example.domain.User",
                "com.example.domain.Result",
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example.test",
                interfaceName = "UserService",
                methods = emptyList(),
                imports = imports,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "import com.example.domain.User")
        assertContains(result, "import com.example.domain.Result")
    }

    @Test
    fun `GIVEN generateCompleteFake WHEN header provided THEN includes header comment`() {
        // GIVEN
        val header = "Generated by Fakt"

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "Service",
                methods = emptyList(),
                header = header,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "// Generated by Fakt")
    }

    @Test
    fun `GIVEN generateCompleteFake WHEN empty interface THEN generates minimal fake`() {
        // GIVEN - no methods or properties

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "EmptyService",
                methods = emptyList(),
                properties = emptyList(),
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "package com.example")
        assertContains(result, "class FakeEmptyServiceImpl : EmptyService {")
        assertContains(result, "}")
    }

    @Test
    fun `GIVEN generateCompleteFake WHEN complete interface THEN generates full fake implementation`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec("getUser", listOf(Triple("id", "String", false)), "User?"),
                MethodSpec("getAllUsers", emptyList(), "List<User>"),
                MethodSpec(
                    "saveUser",
                    listOf(Triple("user", "User", false)),
                    "Result<Unit>",
                    isSuspend = true,
                ),
            )

        val properties =
            listOf(
                PropertySpec("users", "StateFlow<List<User>>", isStateFlow = true),
                PropertySpec("count", "Int", isStateFlow = false),
            )

        val imports =
            listOf(
                "com.example.domain.User",
                "com.example.domain.Result",
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example.test",
                interfaceName = "UserService",
                methods = methods,
                properties = properties,
                imports = imports,
                header = "Generated by Fakt",
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Header
        assertContains(result, "// Generated by Fakt")
        assertContains(result, "package com.example.test")

        // THEN - Imports
        assertContains(result, "import com.example.domain.User")
        assertContains(result, "import com.example.domain.Result")
        assertContains(result, "import kotlinx.coroutines.flow.StateFlow")
        assertContains(result, "import kotlinx.coroutines.flow.MutableStateFlow")

        // THEN - Class declaration
        assertContains(result, "class FakeUserServiceImpl : UserService")

        // THEN - Properties
        assertContains(
            result,
            "private val usersValue: StateFlow<List<User>> = MutableStateFlow(emptyList())",
        )
        assertContains(result, "override val users: StateFlow<List<User>>")
        assertContains(result, "private val countValue: Int = 0")
        assertContains(result, "override val count: Int")

        // THEN - Methods
        assertContains(result, "private var getUserBehavior: (String) -> User? = { null }")
        assertContains(result, "override fun getUser(id: String): User?")
        assertContains(
            result,
            "private var getAllUsersBehavior: () -> List<User> = { emptyList() }",
        )
        assertContains(result, "override fun getAllUsers(): List<User>")
        assertContains(
            result,
            "private var saveUserBehavior: suspend (User) -> Result<Unit> = { Result.success(Unit) }",
        )
        assertContains(result, "override suspend fun saveUser(user: User): Result<Unit>")

        // THEN - Configuration methods
        assertContains(result, "internal fun configureGetUser(behavior: (String) -> User?): Unit")
        assertContains(
            result,
            "internal fun configureGetAllUsers(behavior: () -> List<User>): Unit",
        )
        assertContains(
            result,
            "internal fun configureSaveUser(behavior: suspend (User) -> Result<Unit>): Unit",
        )
    }
}
