// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import com.rsicarelli.fakt.compiler.fir.metadata.FirVisibility
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for CallHistoryGenerator functions.
 *
 * Validates call history generation for:
 * - Methods with parameters (data class storage)
 * - Methods without parameters (Unit storage)
 * - Vararg-only methods (Unit storage)
 * - Verifier classes (full and simplified)
 * - Verify extension functions
 * - Generic type erasure
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CallHistoryGeneratorTest {
    // region resolveHistoryStorageType

    @Test
    fun `GIVEN method with single param WHEN resolveHistoryStorageType THEN returns data class info`() {
        // GIVEN
        val interfaceName = "UserService"
        val methodName = "getUser"
        val params = listOf(Triple("id", "String", false))

        // WHEN
        val result = resolveHistoryStorageType(interfaceName, methodName, params)

        // THEN
        assertNotNull(result.dataClassName)
        assertEquals("UserServiceGetUserCall", result.dataClassName)
        assertEquals(1, result.params.size)
        assertEquals("id", result.params[0].first)
        assertFalse(result.isUnitStorage)
    }

    @Test
    fun `GIVEN method with multiple params WHEN resolveHistoryStorageType THEN returns data class with all params`() {
        // GIVEN
        val interfaceName = "OrderService"
        val methodName = "createOrder"
        val params =
            listOf(
                Triple("userId", "String", false),
                Triple("amount", "Int", false),
                Triple("currency", "String", false),
            )

        // WHEN
        val result = resolveHistoryStorageType(interfaceName, methodName, params)

        // THEN
        assertNotNull(result.dataClassName)
        assertEquals("OrderServiceCreateOrderCall", result.dataClassName)
        assertEquals(3, result.params.size)
        assertFalse(result.isUnitStorage)
    }

    @Test
    fun `GIVEN method without params WHEN resolveHistoryStorageType THEN returns Unit storage`() {
        // GIVEN
        val interfaceName = "AuthService"
        val methodName = "logout"
        val params = emptyList<Triple<String, String, Boolean>>()

        // WHEN
        val result = resolveHistoryStorageType(interfaceName, methodName, params)

        // THEN
        assertNull(result.dataClassName)
        assertTrue(result.params.isEmpty())
        assertTrue(result.isUnitStorage)
    }

    @Test
    fun `GIVEN vararg-only method WHEN resolveHistoryStorageType THEN returns Unit storage`() {
        // GIVEN
        val interfaceName = "LogService"
        val methodName = "log"
        val params = listOf(Triple("messages", "Array<out String>", true)) // vararg = true

        // WHEN
        val result = resolveHistoryStorageType(interfaceName, methodName, params)

        // THEN
        assertNull(result.dataClassName)
        assertTrue(result.params.isEmpty())
        assertTrue(result.isUnitStorage)
    }

    @Test
    fun `GIVEN method with regular and vararg params WHEN resolveHistoryStorageType THEN only captures regular params`() {
        // GIVEN
        val interfaceName = "FormatService"
        val methodName = "format"
        val params =
            listOf(
                Triple("template", "String", false),
                Triple("args", "Array<out Any>", true), // vararg excluded
            )

        // WHEN
        val result = resolveHistoryStorageType(interfaceName, methodName, params)

        // THEN
        assertNotNull(result.dataClassName)
        assertEquals("FormatServiceFormatCall", result.dataClassName)
        assertEquals(1, result.params.size)
        assertEquals("template", result.params[0].first)
        assertFalse(result.isUnitStorage)
    }

    // endregion

    // region buildHistoryRecordExpression

    @Test
    fun `GIVEN method with params WHEN buildHistoryRecordExpression THEN returns data class instantiation`() {
        // GIVEN
        val interfaceName = "UserService"
        val methodName = "getUser"
        val params = listOf(Triple("id", "String", false))

        // WHEN
        val result = buildHistoryRecordExpression(interfaceName, methodName, params)

        // THEN
        assertEquals("UserServiceGetUserCall(id)", result)
    }

    @Test
    fun `GIVEN method with multiple params WHEN buildHistoryRecordExpression THEN includes all param names`() {
        // GIVEN
        val interfaceName = "OrderService"
        val methodName = "createOrder"
        val params =
            listOf(
                Triple("userId", "String", false),
                Triple("amount", "Int", false),
            )

        // WHEN
        val result = buildHistoryRecordExpression(interfaceName, methodName, params)

        // THEN
        assertEquals("OrderServiceCreateOrderCall(userId, amount)", result)
    }

    @Test
    fun `GIVEN 0-param method WHEN buildHistoryRecordExpression THEN returns Unit`() {
        // GIVEN
        val interfaceName = "AuthService"
        val methodName = "logout"
        val params = emptyList<Triple<String, String, Boolean>>()

        // WHEN
        val result = buildHistoryRecordExpression(interfaceName, methodName, params)

        // THEN
        assertEquals("Unit", result)
    }

    @Test
    fun `GIVEN vararg-only method WHEN buildHistoryRecordExpression THEN returns Unit`() {
        // GIVEN
        val interfaceName = "LogService"
        val methodName = "log"
        val params = listOf(Triple("messages", "Array<out String>", true))

        // WHEN
        val result = buildHistoryRecordExpression(interfaceName, methodName, params)

        // THEN
        assertEquals("Unit", result)
    }

    // endregion

    // region generateUnitVerifierClass

    @Test
    fun `GIVEN 0-param method WHEN generateUnitVerifierClass THEN generates simplified verifier`() {
        // GIVEN
        val interfaceName = "AuthService"
        val methodName = "logout"

        // WHEN
        val result = generateUnitVerifierClass(interfaceName, methodName, FirVisibility.PUBLIC)

        // THEN
        assertTrue(result.contains("class AuthServiceLogoutCallVerifier"))
        assertTrue(result.contains("private val calls: List<Unit>"))
        assertTrue(result.contains("fun wasCalledTimes(n: Int): Boolean = calls.size == n"))
        assertTrue(result.contains("fun wasNeverCalled(): Boolean = calls.isEmpty()"))
        // Should NOT contain wasCalledWith, wasCalledInOrder, accessor properties
        assertFalse(result.contains("fun wasCalledWith"))
        assertFalse(result.contains("fun wasCalledInOrder"))
        assertFalse(result.contains("val lastOrNull"))
        assertFalse(result.contains("val first"))
        assertFalse(result.contains("val all"))
    }

    @Test
    fun `GIVEN internal visibility WHEN generateUnitVerifierClass THEN uses internal modifier`() {
        // GIVEN
        val interfaceName = "InternalService"
        val methodName = "doWork"

        // WHEN
        val result = generateUnitVerifierClass(interfaceName, methodName, FirVisibility.INTERNAL)

        // THEN
        assertTrue(result.contains("internal class InternalServiceDoWorkCallVerifier"))
        assertTrue(result.contains("internal fun wasCalledTimes"))
        assertTrue(result.contains("internal fun wasNeverCalled"))
    }

    // endregion

    // region generateUnitVerifyFunction

    @Test
    fun `GIVEN 0-param method WHEN generateUnitVerifyFunction THEN generates verify extension`() {
        // GIVEN
        val fakeClassName = "FakeAuthServiceImpl"
        val interfaceName = "AuthService"
        val methodName = "logout"

        // WHEN
        val result =
            generateUnitVerifyFunction(
                fakeClassName,
                interfaceName,
                methodName,
                FirVisibility.PUBLIC,
            )

        // THEN
        assertTrue(result.contains("inline fun FakeAuthServiceImpl.verifyLogout"))
        assertTrue(result.contains("block: AuthServiceLogoutCallVerifier.() -> Unit"))
        assertTrue(result.contains("AuthServiceLogoutCallVerifier(_logoutCalls.value).apply(block)"))
    }

    @Test
    fun `GIVEN generic class WHEN generateUnitVerifyFunction THEN includes type parameters`() {
        // GIVEN
        val fakeClassName = "FakeRepositoryImpl"
        val interfaceName = "Repository"
        val methodName = "clear"
        val typeParams = listOf("T")

        // WHEN
        val result =
            generateUnitVerifyFunction(
                fakeClassName,
                interfaceName,
                methodName,
                FirVisibility.PUBLIC,
                typeParams,
            )

        // THEN
        assertTrue(result.contains("<T>"))
        assertTrue(result.contains("FakeRepositoryImpl<T>.verifyClear"))
    }

    // endregion

    // region generateCallHistoryComponents integration

    @Test
    fun `GIVEN interface with mixed methods WHEN generateCallHistoryComponents THEN generates all components`() {
        // GIVEN
        val fakeClassName = "FakeUserServiceImpl"
        val interfaceName = "UserService"
        val methods =
            listOf(
                MethodSpec(
                    name = "getUser",
                    params = listOf(Triple("id", "String", false)),
                    returnType = "User",
                ),
                MethodSpec(
                    name = "logout",
                    params = emptyList(),
                    returnType = "Unit",
                ),
                MethodSpec(
                    name = "log",
                    params = listOf(Triple("messages", "Array<out String>", true)),
                    returnType = "Unit",
                ),
            )

        // WHEN
        val result =
            generateCallHistoryComponents(
                fakeClassName,
                interfaceName,
                methods,
                FirVisibility.PUBLIC,
            )

        // THEN
        // Data class only for getUser (has params)
        assertTrue(result.contains("data class UserServiceGetUserCall"))
        assertFalse(result.contains("data class UserServiceLogoutCall")) // No data class for 0-param
        assertFalse(result.contains("data class UserServiceLogCall")) // No data class for vararg-only

        // Full verifier for getUser
        assertTrue(result.contains("class UserServiceGetUserCallVerifier"))
        assertTrue(result.contains("fun wasCalledWith(id: String)"))

        // Simplified verifiers for 0-param/vararg-only
        assertTrue(result.contains("class UserServiceLogoutCallVerifier(private val calls: List<Unit>)"))
        assertTrue(result.contains("class UserServiceLogCallVerifier(private val calls: List<Unit>)"))

        // Verify functions for ALL methods
        assertTrue(result.contains("fun FakeUserServiceImpl.verifyGetUser"))
        assertTrue(result.contains("fun FakeUserServiceImpl.verifyLogout"))
        assertTrue(result.contains("fun FakeUserServiceImpl.verifyLog"))
    }

    @Test
    fun `GIVEN empty methods list WHEN generateCallHistoryComponents THEN returns empty string`() {
        // GIVEN
        val methods = emptyList<MethodSpec>()

        // WHEN
        val result =
            generateCallHistoryComponents(
                "FakeServiceImpl",
                "Service",
                methods,
                FirVisibility.PUBLIC,
            )

        // THEN
        assertEquals("", result)
    }

    @Test
    fun `GIVEN only 0-param methods WHEN generateCallHistoryComponents THEN skips data classes region`() {
        // GIVEN
        val methods =
            listOf(
                MethodSpec(name = "start", params = emptyList(), returnType = "Unit"),
                MethodSpec(name = "stop", params = emptyList(), returnType = "Unit"),
            )

        // WHEN
        val result =
            generateCallHistoryComponents(
                "FakeServiceImpl",
                "Service",
                methods,
                FirVisibility.PUBLIC,
            )

        // THEN
        assertFalse(result.contains("// region Call History Data Classes"))
        assertTrue(result.contains("// region Call History Verifiers"))
        assertTrue(result.contains("class ServiceStartCallVerifier"))
        assertTrue(result.contains("class ServiceStopCallVerifier"))
    }

    // endregion

    // region Generic type erasure

    @Test
    fun `GIVEN generic method WHEN generateCallDataClass THEN erases type params to Any`() {
        // GIVEN
        val interfaceName = "Repository"
        val methodName = "save"
        val params = listOf(Triple("item", "T", false))
        val typeParams = setOf("T")

        // WHEN
        val result = generateCallDataClass(interfaceName, methodName, params, FirVisibility.PUBLIC, typeParams)

        // THEN
        assertTrue(result.contains("data class RepositorySaveCall"))
        assertTrue(result.contains("val item: Any?"))
        assertFalse(result.contains("val item: T"))
    }

    @Test
    fun `GIVEN method with generic collection param WHEN generateCallDataClass THEN erases nested type params`() {
        // GIVEN
        val interfaceName = "BatchService"
        val methodName = "process"
        val params = listOf(Triple("items", "List<T>", false))
        val typeParams = setOf("T")

        // WHEN
        val result = generateCallDataClass(interfaceName, methodName, params, FirVisibility.PUBLIC, typeParams)

        // THEN
        assertTrue(result.contains("val items: List<Any?>"))
    }

    @Test
    fun `GIVEN method with multiple generic params WHEN generateCallDataClass THEN erases all type params`() {
        // GIVEN
        val interfaceName = "MapService"
        val methodName = "transform"
        val params =
            listOf(
                Triple("key", "K", false),
                Triple("value", "V", false),
            )
        val typeParams = setOf("K", "V")

        // WHEN
        val result = generateCallDataClass(interfaceName, methodName, params, FirVisibility.PUBLIC, typeParams)

        // THEN
        assertTrue(result.contains("val key: Any?"))
        assertTrue(result.contains("val value: Any?"))
    }

    // endregion

    // region Verifier with params

    @Test
    fun `GIVEN single param method WHEN generateVerifierClass THEN includes wasCalledInOrder`() {
        // GIVEN
        val interfaceName = "UserService"
        val methodName = "getUser"
        val params = listOf(Triple("id", "String", false))

        // WHEN
        val result = generateVerifierClass(interfaceName, methodName, params, FirVisibility.PUBLIC, emptySet())

        // THEN
        assertTrue(result.contains("fun wasCalledInOrder(vararg ids: String)"))
        assertTrue(result.contains("fun neverCalledWith(id: String)"))
    }

    @Test
    fun `GIVEN multi param method WHEN generateVerifierClass THEN excludes wasCalledInOrder`() {
        // GIVEN
        val interfaceName = "OrderService"
        val methodName = "create"
        val params =
            listOf(
                Triple("userId", "String", false),
                Triple("amount", "Int", false),
            )

        // WHEN
        val result = generateVerifierClass(interfaceName, methodName, params, FirVisibility.PUBLIC, emptySet())

        // THEN
        assertFalse(result.contains("wasCalledInOrder"))
        assertTrue(result.contains("fun wasCalledWith(userId: String, amount: Int)"))
    }

    // endregion
}
