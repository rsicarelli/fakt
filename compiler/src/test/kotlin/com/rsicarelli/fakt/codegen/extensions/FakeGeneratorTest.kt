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
 * Validates complete fake generation with immutable constructor-based behaviors, override methods,
 * and configuration DSL.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FakeGeneratorTest {
    @Test
    fun `GIVEN generateCompleteFake WHEN simple method THEN generates immutable implementation`() {
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

        // THEN - Class declaration
        assertContains(result, "package com.example")
        assertContains(result, "class FakeUserServiceImpl")

        // THEN - Direct private val constructor property with default
        assertContains(result, "private val getUserBehavior: (String) -> User? = { p0 -> null }")

        // THEN - No intermediate member properties (simplified pattern)
        assertFalse(
            result.contains("getUserBehavior ?:"),
            "Should NOT have intermediate member with null-coalescing (simplified pattern)",
        )

        // THEN - Override delegates to behavior
        assertContains(result, "override fun getUser(id: String): User? {")
        assertContains(result, "return getUserBehavior(id)")

        // THEN - No configureXxx methods (immutable)
        assertFalse(
            result.contains("internal fun configureGetUser"),
            "Should NOT contain configureXxx methods in immutable fake",
        )
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

        // THEN - Direct private val constructor property with suspend default
        assertContains(result, "private val saveUserBehavior: suspend (User) -> Result<Unit>")
        assertContains(result, "Result.success(Unit)")

        // THEN - Override method
        assertContains(result, "override suspend fun saveUser(user: User): Result<Unit> {")
        assertContains(result, "return saveUserBehavior(user)")

        // THEN - No configureXxx
        assertFalse(result.contains("internal fun configureSaveUser"))
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
    fun `GIVEN generateCompleteFake WHEN simple property THEN generates immutable constructor param`() {
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

        // THEN - Direct private val constructor property with default
        assertContains(result, "private val countBehavior: () -> Int = { 0 }")

        // THEN - Override delegates (block body due to call tracking)
        assertContains(result, "override val count: Int")
        assertContains(result, "countCalls.update { it + Unit }")
        assertContains(result, "return countBehavior()")
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

        // THEN - All behaviors as immutable members
        assertContains(result, "private val getUserBehavior")
        assertContains(result, "override fun getUser(id: String): User?")
        assertContains(result, "private val saveUserBehavior: suspend")
        assertContains(result, "override suspend fun saveUser(user: User): Result<Unit>")
        assertContains(result, "private val deleteUserBehavior")
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
    fun `GIVEN generateCompleteFake WHEN complete interface THEN generates full immutable fake`() {
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

        // THEN - Header and package
        assertContains(result, "// Generated by Fakt")
        assertContains(result, "package com.example.test")

        // THEN - Imports
        assertContains(result, "import com.example.domain.User")
        assertContains(result, "import com.example.domain.Result")
        assertContains(result, "import kotlinx.coroutines.flow.StateFlow")
        assertContains(result, "import kotlinx.coroutines.flow.MutableStateFlow")

        // THEN - Class declaration
        assertContains(result, "class FakeUserServiceImpl")

        // THEN - StateFlow property (unchanged pattern)
        assertContains(
            result,
            "private val usersValue: StateFlow<List<User>> = MutableStateFlow(emptyList())",
        )
        assertContains(result, "override val users: StateFlow<List<User>>")

        // THEN - Simple property as immutable member
        assertContains(result, "private val countBehavior: () -> Int")
        assertContains(result, "override val count: Int")

        // THEN - Method behaviors as immutable members
        assertContains(result, "private val getUserBehavior: (String) -> User?")
        assertContains(result, "override fun getUser(id: String): User?")
        assertContains(result, "private val getAllUsersBehavior: () -> List<User>")
        assertContains(result, "override fun getAllUsers(): List<User>")
        assertContains(result, "private val saveUserBehavior: suspend (User) -> Result<Unit>")
        assertContains(result, "override suspend fun saveUser(user: User): Result<Unit>")

        // THEN - No configuration methods (immutable)
        assertFalse(result.contains("internal fun configure"))
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
        assertContains(result, "public class FakePublicServiceImpl")
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
        assertContains(result, "internal class FakeInternalServiceImpl")
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
        assertContains(result, "public class FakeDefaultServiceImpl")
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
        assertContains(result, "public class FakePrivateServiceImpl")
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
        assertContains(result, "public class FakeProtectedServiceImpl")
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
            result.contains("getUserCalls"),
            "Should not contain getUserCalls backing field when call history is disabled",
        )
    }

    @Test
    fun `GIVEN generateCallHistory false WHEN generating fake with method THEN omits call tracking region`() {
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

        // THEN - Should NOT contain call history region
        assertFalse(
            result.contains("//region Call History"),
            "Should not contain Call History region when call history is disabled",
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
        assertContains(result, "getUserCalls")
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
        assertContains(result, "doWorkCalls")
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

        // THEN - Should NOT contain any call history fields
        assertFalse(result.contains("getUserCalls"))
        assertFalse(result.contains("saveUserCalls"))
        assertFalse(result.contains("deleteUserCalls"))
    }

    @Test
    fun `GIVEN generateCallHistory false WHEN generating fake THEN still generates immutable behavior properties`() {
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

        // THEN - Should still contain immutable behavior member property
        assertContains(result, "private val doWorkBehavior")
        // THEN - No configureXxx methods (immutable)
        assertFalse(result.contains("internal fun configureDoWork"))
        // THEN - Override method still works
        assertContains(result, "override fun doWork(value: Int): String")
    }

    // ==========================================
    // Mutable Property Tests (Getter/Setter Pattern)
    // ==========================================

    @Test
    fun `GIVEN mutable property WHEN generating fake THEN creates getter and setter constructor params`() {
        // GIVEN
        val properties =
            listOf(
                PropertySpec(
                    name = "currentUser",
                    type = "User?",
                    isStateFlow = false,
                    isMutable = true,
                )
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

        // THEN - Getter constructor property with default
        assertContains(result, "private val currentUserGetter: () -> User? = { null }")

        // THEN - Setter constructor property with no-op default
        assertContains(result, "private val currentUserSetter: (User?) -> Unit = { }")

        // THEN - Override mutable property with getter and setter (block body due to call tracking)
        assertContains(result, "override var currentUser: User?")
        assertContains(result, "currentUserCalls.update { it + Unit }")
        assertContains(result, "return currentUserGetter()")
        assertContains(result, "setCurrentUserCalls.update { it + Unit }")
        assertContains(result, "currentUserSetter(value)")
    }

    @Test
    fun `GIVEN mutable non-nullable property WHEN generating fake THEN creates typed getter and setter`() {
        // GIVEN
        val properties =
            listOf(
                PropertySpec(name = "count", type = "Int", isStateFlow = false, isMutable = true)
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

        // THEN - Getter with typed default
        assertContains(result, "private val countGetter: () -> Int = { 0 }")

        // THEN - Setter with no-op default
        assertContains(result, "private val countSetter: (Int) -> Unit = { }")

        // THEN - Override var property
        assertContains(result, "override var count: Int")
    }

    @Test
    fun `GIVEN mix of mutable and immutable properties WHEN generating fake THEN handles both patterns`() {
        // GIVEN
        val properties =
            listOf(
                PropertySpec(
                    name = "name",
                    type = "String",
                    isStateFlow = false,
                    isMutable = false,
                ),
                PropertySpec(name = "score", type = "Int", isStateFlow = false, isMutable = true),
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "Player",
                properties = properties,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Immutable property uses single behavior
        assertContains(result, "private val nameBehavior: () -> String")
        assertContains(result, "override val name: String")

        // THEN - Mutable property uses getter/setter pair
        assertContains(result, "private val scoreGetter: () -> Int")
        assertContains(result, "private val scoreSetter: (Int) -> Unit")
        assertContains(result, "override var score: Int")
    }

    // ==========================================
    // Open Method/Property Tests (Class Fakes)
    // ==========================================

    @Test
    fun `GIVEN abstract method in class WHEN generating fake THEN creates non-null constructor param`() {
        // GIVEN - isAbstract = true means it MUST be overridden (like interface methods)
        val methods =
            listOf(
                MethodSpec(
                    name = "validate",
                    params = listOf(Triple("input", "String", false)),
                    returnType = "Boolean",
                    isAbstract = true,
                )
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "Validator",
                methods = methods,
                isClass = true,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Abstract method gets non-null constructor property with error default
        // (abstract class methods require explicit configuration via DSL)
        assertContains(result, "private val validateBehavior: (String) -> Boolean = { p0 -> error(")
        assertContains(result, "Abstract method 'validate(String): Boolean'")
    }

    @Test
    fun `GIVEN open method in class WHEN generating fake THEN creates nullable constructor param`() {
        // GIVEN - isAbstract = false + isClass = true → open method
        val methods =
            listOf(
                MethodSpec(
                    name = "validate",
                    params = listOf(Triple("input", "String", false)),
                    returnType = "Boolean",
                    isAbstract = false, // open, not abstract
                )
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "Validator",
                methods = methods,
                isClass = true,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Open method gets nullable constructor property (null = delegate to super)
        assertContains(result, "private val validateBehavior: ((String) -> Boolean)? = null")
    }

    @Test
    fun `GIVEN open property in class WHEN generating fake THEN creates nullable constructor param`() {
        // GIVEN
        val properties =
            listOf(
                PropertySpec(
                    name = "label",
                    type = "String",
                    isStateFlow = false,
                    isAbstract = false, // open, not abstract
                )
            )

        // WHEN
        val file =
            generateCompleteFake(
                packageName = "com.example",
                interfaceName = "Widget",
                properties = properties,
                isClass = true,
            )
        val builder = CodeBuilder()
        file.renderTo(builder)
        val result = builder.build()

        // THEN - Open property gets nullable constructor property
        assertContains(result, "private val labelBehavior: (() -> String)? = null")
    }
}
