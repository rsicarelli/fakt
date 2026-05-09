// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Tests for KDoc generation in factory functions.
 *
 * Validates that generated KDoc is developer-friendly and includes all necessary information for
 * IDE autocomplete.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KDocGeneratorTest {
    // ==========================================
    // Basic KDoc Structure Tests
    // ==========================================

    @Test
    fun `GIVEN interface name WHEN generating KDoc THEN includes interface reference`() {
        // GIVEN
        val interfaceName = "UserService"

        // WHEN
        val kdoc = generateFactoryKDoc(interfaceName)

        // THEN
        assertContains(kdoc, "[UserService]")
        assertContains(kdoc, "Creates a fake implementation of [UserService] for testing.")
    }

    @Test
    fun `GIVEN interface name WHEN generating KDoc THEN includes proper KDoc markers`() {
        // GIVEN
        val interfaceName = "UserService"

        // WHEN
        val kdoc = generateFactoryKDoc(interfaceName)

        // THEN
        assertTrue(kdoc.startsWith("/**"))
        assertTrue(kdoc.endsWith(" */"))
    }

    @Test
    fun `GIVEN interface name WHEN generating KDoc THEN includes param and return tags`() {
        // GIVEN
        val interfaceName = "UserService"

        // WHEN
        val kdoc = generateFactoryKDoc(interfaceName)

        // THEN
        assertContains(kdoc, "@param configure")
        assertContains(kdoc, "@return")
        assertContains(kdoc, "[FakeUserServiceImpl]")
    }

    @Test
    fun `GIVEN interface name WHEN generating KDoc THEN includes see reference`() {
        // GIVEN
        val interfaceName = "UserService"

        // WHEN
        val kdoc = generateFactoryKDoc(interfaceName)

        // THEN
        assertContains(kdoc, "@see UserService")
    }

    // ==========================================
    // Method Documentation Tests
    // ==========================================

    @Test
    fun `GIVEN interface with single method WHEN generating KDoc THEN includes method in behaviors section`() {
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
        val kdoc = generateFactoryKDoc("UserService", methods = methods)

        // THEN
        assertContains(kdoc, "## Configurable Behaviors")
        assertContains(kdoc, "`getUser`: (id: String) -> User?")
    }

    @Test
    fun `GIVEN interface with suspend method WHEN generating KDoc THEN marks method as suspend`() {
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
        val kdoc = generateFactoryKDoc("UserService", methods = methods)

        // THEN
        assertContains(kdoc, "`saveUser`: (user: User) -> Result<Unit> (suspend)")
    }

    @Test
    fun `GIVEN interface with operator method WHEN generating KDoc THEN marks method as operator`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(
                    name = "get",
                    params = listOf(Triple("index", "Int", false)),
                    returnType = "String",
                    isOperator = true,
                )
            )

        // WHEN
        val kdoc = generateFactoryKDoc("Container", methods = methods)

        // THEN
        assertContains(kdoc, "`get`: (index: Int) -> String (operator)")
    }

    @Test
    fun `GIVEN interface with extension method WHEN generating KDoc THEN marks method as extension`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(
                    name = "plus",
                    params = listOf(Triple("other", "Vector", false)),
                    returnType = "Vector",
                    extensionReceiverType = "Vector",
                )
            )

        // WHEN
        val kdoc = generateFactoryKDoc("VectorOps", methods = methods)

        // THEN
        assertContains(kdoc, "`plus`: (receiver: Vector, other: Vector) -> Vector (extension)")
    }

    @Test
    fun `GIVEN interface with no-param method WHEN generating KDoc THEN shows empty params`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(name = "getAllUsers", params = emptyList(), returnType = "List<User>")
            )

        // WHEN
        val kdoc = generateFactoryKDoc("UserService", methods = methods)

        // THEN
        assertContains(kdoc, "`getAllUsers`: () -> List<User>")
    }

    @Test
    fun `GIVEN interface with multiple param method WHEN generating KDoc THEN shows all params`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(
                    name = "updateUser",
                    params =
                        listOf(
                            Triple("id", "String", false),
                            Triple("name", "String", false),
                            Triple("email", "String?", false),
                        ),
                    returnType = "User",
                )
            )

        // WHEN
        val kdoc = generateFactoryKDoc("UserService", methods = methods)

        // THEN
        assertContains(kdoc, "`updateUser`: (id: String, name: String, email: String?) -> User")
    }

    // ==========================================
    // Property Documentation Tests
    // ==========================================

    @Test
    fun `GIVEN interface with val property WHEN generating KDoc THEN includes property in behaviors`() {
        // GIVEN
        val properties = listOf(PropertySpec(name = "count", type = "Int", isMutable = false))

        // WHEN
        val kdoc = generateFactoryKDoc("Counter", properties = properties)

        // THEN
        assertContains(kdoc, "## Configurable Behaviors")
        assertContains(kdoc, "`count`: Int")
        assertFalse(kdoc.contains("`count`: Int (mutable)"))
    }

    @Test
    fun `GIVEN interface with var property WHEN generating KDoc THEN marks as mutable`() {
        // GIVEN
        val properties = listOf(PropertySpec(name = "name", type = "String", isMutable = true))

        // WHEN
        val kdoc = generateFactoryKDoc("UserProfile", properties = properties)

        // THEN
        assertContains(kdoc, "`name`: String (mutable)")
    }

    @Test
    fun `GIVEN interface with StateFlow property WHEN generating KDoc THEN excludes from behaviors`() {
        // GIVEN
        val properties =
            listOf(PropertySpec(name = "users", type = "StateFlow<List<User>>", isStateFlow = true))

        // WHEN
        val kdoc = generateFactoryKDoc("UserStore", properties = properties)

        // THEN
        // StateFlow properties have a different API (direct value access), so they're excluded
        assertFalse(kdoc.contains("`users`:"))
    }

    // ==========================================
    // Usage Example Tests
    // ==========================================

    @Test
    fun `GIVEN interface with methods WHEN generating KDoc THEN includes usage example`() {
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
        val kdoc = generateFactoryKDoc("UserService", methods = methods)

        // THEN
        assertContains(kdoc, "Example:")
        assertContains(kdoc, "```kotlin")
        assertContains(kdoc, "val fake = fakeUserService {")
        assertContains(kdoc, "getUser {")
    }

    @Test
    fun `GIVEN interface with no param method WHEN generating KDoc THEN shows simple example`() {
        // GIVEN
        val methods =
            listOf(MethodSpec(name = "getAll", params = emptyList(), returnType = "List<String>"))

        // WHEN
        val kdoc = generateFactoryKDoc("Service", methods = methods)

        // THEN
        assertContains(kdoc, "getAll { listOf(/* items */)")
    }

    @Test
    fun `GIVEN interface with Unit return method WHEN generating KDoc THEN shows side effect comment`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(
                    name = "logEvent",
                    params = listOf(Triple("event", "String", false)),
                    returnType = "Unit",
                )
            )

        // WHEN
        val kdoc = generateFactoryKDoc("Logger", methods = methods)

        // THEN
        assertContains(kdoc, "/* configure side effects */")
    }

    @Test
    fun `GIVEN empty interface WHEN generating KDoc THEN generates minimal documentation`() {
        // GIVEN - no methods or properties

        // WHEN
        val kdoc = generateFactoryKDoc("EmptyService")

        // THEN
        assertContains(kdoc, "Creates a fake implementation of [EmptyService] for testing.")
        assertContains(kdoc, "@param configure")
        assertContains(kdoc, "@return")
        // No Example section for empty interfaces
        assertFalse(kdoc.contains("## Configurable Behaviors"))
    }

    // ==========================================
    // Integration with Factory Function Tests
    // ==========================================

    @Test
    fun `GIVEN method specs WHEN generating factory with KDoc THEN includes full documentation`() {
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
        val factory =
            generateFactoryFunction(
                FactoryFunctionSpec(interfaceName = "UserService", methods = methods),
                includeKDoc = true,
            )

        // THEN
        assertContains(factory, "/**")
        assertContains(factory, "Creates a fake implementation of [UserService]")
        assertContains(factory, "`getUser`: (id: String) -> User?")
        assertContains(factory, "inline fun fakeUserService")
    }

    @Test
    fun `GIVEN includeKDoc false WHEN generating factory THEN excludes KDoc`() {
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
        val factory =
            generateFactoryFunction(
                FactoryFunctionSpec(interfaceName = "UserService", methods = methods),
                includeKDoc = false,
            )

        // THEN
        assertFalse(factory.contains("/**"))
        assertFalse(factory.contains("Creates a fake implementation"))
        assertTrue(factory.contains("inline fun fakeUserService"))
    }

    @Test
    fun `GIVEN complex interface WHEN generating factory THEN includes complete KDoc`() {
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

        val properties =
            listOf(
                PropertySpec("userCount", "Int"),
                PropertySpec("currentUser", "User?", isMutable = true),
            )

        // WHEN
        val factory =
            generateFactoryFunction(
                FactoryFunctionSpec(
                    interfaceName = "UserRepository",
                    methods = methods,
                    properties = properties,
                ),
                includeKDoc = true,
            )

        // THEN
        assertContains(factory, "Creates a fake implementation of [UserRepository]")
        assertContains(factory, "`getUser`: (id: String) -> User?")
        assertContains(factory, "`saveUser`: (user: User) -> Result<Unit> (suspend)")
        assertContains(factory, "`deleteUser`: (id: String) -> Unit")
        assertContains(factory, "`userCount`: Int")
        assertContains(factory, "`currentUser`: User? (mutable)")
        assertContains(factory, "@see UserRepository")
    }

    // ==========================================
    // Edge Cases and Special Types
    // ==========================================

    @Test
    fun `GIVEN Result return type WHEN generating example THEN shows Result success pattern`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(
                    name = "save",
                    params = listOf(Triple("data", "String", false)),
                    returnType = "Result<Int>",
                )
            )

        // WHEN
        val kdoc = generateFactoryKDoc("DataStore", methods = methods)

        // THEN
        assertContains(kdoc, "Result.success(/* value */)")
    }

    @Test
    fun `GIVEN nullable return type WHEN generating example THEN shows null pattern`() {
        // GIVEN
        val methods =
            listOf(MethodSpec(name = "find", params = emptyList(), returnType = "String?"))

        // WHEN
        val kdoc = generateFactoryKDoc("Finder", methods = methods)

        // THEN
        assertContains(kdoc, "null")
    }

    @Test
    fun `GIVEN Map return type WHEN generating example THEN shows mapOf pattern`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(name = "getAll", params = emptyList(), returnType = "Map<String, Int>")
            )

        // WHEN
        val kdoc = generateFactoryKDoc("Cache", methods = methods)

        // THEN
        assertContains(kdoc, "mapOf(/* entries */)")
    }

    @Test
    fun `GIVEN Boolean return type WHEN generating example THEN shows true`() {
        // GIVEN
        val methods =
            listOf(MethodSpec(name = "isValid", params = emptyList(), returnType = "Boolean"))

        // WHEN
        val kdoc = generateFactoryKDoc("Validator", methods = methods)

        // THEN
        assertContains(kdoc, "true")
    }

    @Test
    fun `GIVEN many methods WHEN generating example THEN shows all methods`() {
        // GIVEN - 5 methods
        val methods =
            (1..5).map { i ->
                MethodSpec(name = "method$i", params = emptyList(), returnType = "Unit")
            }

        // WHEN
        val kdoc = generateFactoryKDoc("LargeService", methods = methods)

        // THEN - Example should show all methods
        val exampleSection = kdoc.substringAfter("Example:").substringBefore("```\n *\n")
        val methodCount = "method\\d".toRegex().findAll(exampleSection).count()
        assertEquals(5, methodCount, "Expected all 5 example methods, got $methodCount")
    }
}
