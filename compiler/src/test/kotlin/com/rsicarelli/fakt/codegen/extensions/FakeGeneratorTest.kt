// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.codegen.renderer.CodeBuilder
import com.rsicarelli.fakt.codegen.renderer.renderTo
import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import org.junit.jupiter.api.TestInstance

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
                )
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
        assertContains(result, "private var getUserBehavior: (String) -> User? = { p0 -> null }")
        assertContains(result, "override fun getUser(id: String): User? {")
        assertContains(result, "return getUserBehavior(id)")
        assertContains(result, "internal fun configureGetUser(behavior: (String) -> User?) = run {")
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
                )
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
            "private var saveUserBehavior: suspend (User) -> Result<Unit> = { p0 -> Result.success(Unit) }",
        )
        assertContains(result, "override suspend fun saveUser(user: User): Result<Unit> {")
        assertContains(result, "return saveUserBehavior(user)")
        assertContains(
            result,
            "internal fun configureSaveUser(behavior: suspend (User) -> Result<Unit>) = run {",
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
                )
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
            listOf(PropertySpec(name = "users", type = "StateFlow<List<User>>", isStateFlow = true))

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
        val properties = listOf(PropertySpec(name = "count", type = "Int", isStateFlow = false))

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
        assertContains(result, "private var countBehavior: () -> Int = { 0 }")
        assertContains(result, "override val count: Int")
        assertContains(result, "get() =")
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
        val imports = listOf("com.example.domain.User", "com.example.domain.Result")

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

        val imports = listOf("com.example.domain.User", "com.example.domain.Result")

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
        assertContains(result, "private var countBehavior: () -> Int = { 0 }")
        assertContains(result, "override val count: Int")

        // THEN - Methods
        assertContains(result, "private var getUserBehavior: (String) -> User? = { p0 -> null }")
        assertContains(result, "override fun getUser(id: String): User?")
        assertContains(
            result,
            "private var getAllUsersBehavior: () -> List<User> = { emptyList() }",
        )
        assertContains(result, "override fun getAllUsers(): List<User>")
        assertContains(
            result,
            "private var saveUserBehavior: suspend (User) -> Result<Unit> = { p0 -> Result.success(Unit) }",
        )
        assertContains(result, "override suspend fun saveUser(user: User): Result<Unit>")

        // THEN - Configuration methods
        assertContains(result, "internal fun configureGetUser(behavior: (String) -> User?) = run {")
        assertContains(
            result,
            "internal fun configureGetAllUsers(behavior: () -> List<User>) = run {",
        )
        assertContains(
            result,
            "internal fun configureSaveUser(behavior: suspend (User) -> Result<Unit>) = run {",
        )
    }

    // ==========================================
    // Visibility Modifier Tests (Issue #21)
    // ==========================================

    @Test
    fun `GIVEN public visibility WHEN generating fake THEN class has public modifier`() {
        // GIVEN
        val methods = listOf(MethodSpec("doWork", emptyList(), "String"))

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "PublicService",
                methods = methods,
                visibility = FirVisibility.PUBLIC,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "public class FakePublicServiceImpl : PublicService")
    }

    @Test
    fun `GIVEN internal visibility WHEN generating fake THEN class has internal modifier`() {
        // GIVEN
        val methods = listOf(MethodSpec("doWork", emptyList(), "String"))

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "InternalService",
                methods = methods,
                visibility = FirVisibility.INTERNAL,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN
        assertContains(result, "internal class FakeInternalServiceImpl : InternalService")
    }

    @Test
    fun `GIVEN default visibility WHEN generating fake THEN class has public modifier`() {
        // GIVEN - no explicit visibility (uses default PUBLIC)
        val methods = listOf(MethodSpec("doWork", emptyList(), "String"))

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "DefaultService",
                methods = methods,
                // visibility not specified - should default to PUBLIC
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should have public modifier for explicitApi() compatibility
        assertContains(result, "public class FakeDefaultServiceImpl : DefaultService")
    }

    @Test
    fun `GIVEN private visibility WHEN generating fake THEN class defaults to public`() {
        // GIVEN - private interfaces can't have public implementations,
        // but we default to public for safety
        val methods = listOf(MethodSpec("doWork", emptyList(), "String"))

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "PrivateService",
                methods = methods,
                visibility = FirVisibility.PRIVATE,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should default to public
        assertContains(result, "public class FakePrivateServiceImpl : PrivateService")
    }

    @Test
    fun `GIVEN protected visibility WHEN generating fake THEN class defaults to public`() {
        // GIVEN - protected is not supported for top-level classes
        val methods = listOf(MethodSpec("doWork", emptyList(), "String"))

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "ProtectedService",
                methods = methods,
                visibility = FirVisibility.PROTECTED,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should default to public
        assertContains(result, "public class FakeProtectedServiceImpl : ProtectedService")
    }

    // ==========================================
    // generateCallHistory Tests (Call History Control)
    // ==========================================

    @Test
    fun `GIVEN generateCallHistory false WHEN generating fake THEN omits call tracking imports`() {
        // GIVEN
        val methods = listOf(MethodSpec("doWork", emptyList(), "String"))

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "LightweightService",
                methods = methods,
                generateCallHistory = false,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should NOT contain MutableStateFlow import used for call tracking
        assertFalse(
            result.contains("import kotlinx.coroutines.flow.MutableStateFlow"),
            "Should not contain MutableStateFlow import when call history is disabled",
        )
        assertFalse(
            result.contains("import kotlinx.coroutines.flow.update"),
            "Should not contain update import when call history is disabled",
        )
    }

    @Test
    fun `GIVEN generateCallHistory false WHEN generating fake THEN omits history backing fields`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(
                    name = "getUser",
                    params = listOf(Triple("id", "String", false)),
                    returnType = "User?",
                )
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "UserService",
                methods = methods,
                generateCallHistory = false,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should NOT contain call tracking backing field
        assertFalse(
            result.contains("_getUserCalls"),
            "Should not contain _getUserCalls backing field when call history is disabled",
        )
        assertFalse(
            result.contains("getUserCallCount"),
            "Should not contain getUserCallCount when call history is disabled",
        )
    }

    @Test
    fun `GIVEN generateCallHistory false WHEN generating fake with method THEN omits Section 2 call tracking`() {
        // GIVEN
        val methods = listOf(MethodSpec("doWork", listOf(Triple("value", "Int", false)), "Unit"))

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "WorkService",
                methods = methods,
                generateCallHistory = false,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should NOT contain Section 2 Call Tracking comment
        assertFalse(
            result.contains("Section 2: Call Tracking"),
            "Should not contain Section 2 Call Tracking when call history is disabled",
        )
        // And should NOT contain the update call in method body
        assertFalse(
            result.contains(".update { it +"),
            "Should not contain history update call when call history is disabled",
        )
    }

    @Test
    fun `GIVEN generateCallHistory true WHEN generating fake THEN includes call tracking`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(
                    name = "getUser",
                    params = listOf(Triple("id", "String", false)),
                    returnType = "User?",
                )
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "UserService",
                methods = methods,
                generateCallHistory = true, // explicitly enabled
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should contain call tracking imports
        assertContains(result, "import kotlinx.coroutines.flow.MutableStateFlow")
        assertContains(result, "import kotlinx.coroutines.flow.update")
        // And should contain backing field
        assertContains(result, "_getUserCalls")
    }

    @Test
    fun `GIVEN generateCallHistory default WHEN generating fake THEN includes call tracking`() {
        // GIVEN - using default generateCallHistory (true)
        val methods = listOf(MethodSpec("doWork", emptyList(), "String"))

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "DefaultService",
                methods = methods,
                // generateCallHistory not specified - should default to true
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should contain call tracking (default behavior)
        assertContains(result, "import kotlinx.coroutines.flow.MutableStateFlow")
        assertContains(result, "_doWorkCalls")
    }

    @Test
    fun `GIVEN generateCallHistory false with multiple methods WHEN generating fake THEN omits all call tracking`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec("getUser", listOf(Triple("id", "String", false)), "User?"),
                MethodSpec(
                    "saveUser",
                    listOf(Triple("user", "User", false)),
                    "Unit",
                    isSuspend = true,
                ),
                MethodSpec("deleteUser", listOf(Triple("id", "String", false)), "Boolean"),
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "UserService",
                methods = methods,
                generateCallHistory = false,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should NOT contain any call tracking fields
        assertFalse(result.contains("_getUserCalls"))
        assertFalse(result.contains("_saveUserCalls"))
        assertFalse(result.contains("_deleteUserCalls"))
        assertFalse(result.contains("CallCount"))
    }

    @Test
    fun `GIVEN generateCallHistory false WHEN generating fake THEN still generates behavior properties`() {
        // GIVEN
        val methods = listOf(MethodSpec("doWork", listOf(Triple("value", "Int", false)), "String"))

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "WorkService",
                methods = methods,
                generateCallHistory = false,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Should still contain behavior property and configure method
        assertContains(result, "private var doWorkBehavior")
        assertContains(result, "internal fun configureDoWork")
        assertContains(result, "override fun doWork(value: Int): String")
    }
}
