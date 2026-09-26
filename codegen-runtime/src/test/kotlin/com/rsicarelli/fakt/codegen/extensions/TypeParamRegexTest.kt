// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.codegen.extensions

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TypeParamRegexTest {
    @Test
    fun `GIVEN unbounded type param as type argument WHEN eraseTypeParamsToAny THEN uses Any`() {
        // GIVEN
        val type = "Map<K, List<V>>"

        // WHEN
        val result = eraseTypeParamsToAny(type, setOf("K", "V"), emptySet())

        // THEN
        assertEquals("Map<Any?, List<Any?>>", result)
    }

    @Test
    fun `GIVEN enum-bounded type param as type argument WHEN eraseTypeParamsToAny THEN star-projects`() {
        // GIVEN
        val type = "(ABTest<T>) -> T"

        // WHEN
        val result = eraseTypeParamsToAny(type, setOf("T"), setOf("T"))

        // THEN
        assertEquals("(ABTest<*>) -> Any?", result)
    }

    @Test
    fun `GIVEN bounded type param nested in generics WHEN eraseTypeParamsToAny THEN star-projects innermost`() {
        // GIVEN
        val type = "Map<String, List<ABTest<T>>>"

        // WHEN
        val result = eraseTypeParamsToAny(type, setOf("T"), setOf("T"))

        // THEN
        assertEquals("Map<String, List<ABTest<*>>>", result)
    }

    @Test
    fun `GIVEN bounded type param as stdlib collection argument WHEN eraseTypeParamsToAny THEN keeps Any`() {
        // GIVEN
        val type = "(List<T>, kotlin.collections.Set<T>) -> Map<String, T>"

        // WHEN
        val result = eraseTypeParamsToAny(type, setOf("T"), setOf("T"))

        // THEN
        assertEquals("(List<Any?>, kotlin.collections.Set<Any?>) -> Map<String, Any?>", result)
    }

    @Test
    fun `GIVEN bounded type param with variance and nullability WHEN eraseTypeParamsToAny THEN star-projects whole argument`() {
        // GIVEN
        val type = "Box<out T?, in T>"

        // WHEN
        val result = eraseTypeParamsToAny(type, setOf("T"), setOf("T"))

        // THEN
        assertEquals("Box<*, *>", result)
    }

    @Test
    fun `GIVEN bounded type param inside function type argument WHEN eraseTypeParamsToAny THEN keeps Any`() {
        // GIVEN
        val type = "Box<(T, Int) -> T>"

        // WHEN
        val result = eraseTypeParamsToAny(type, setOf("T"), setOf("T"))

        // THEN
        assertEquals("Box<(Any?, Int) -> Any?>", result)
    }

    @Test
    fun `GIVEN bounded type param in function params WHEN eraseTypeParamsToAny THEN keeps Any`() {
        // GIVEN
        val type = "(Int, T, String) -> Box<String, T>"

        // WHEN
        val result = eraseTypeParamsToAny(type, setOf("T"), setOf("T"))

        // THEN
        assertEquals("(Int, Any?, String) -> Box<String, *>", result)
    }

    @Test
    fun `GIVEN mixed bounded and unbounded params WHEN eraseTypeParamsToAny THEN only bounded are star-projected`() {
        // GIVEN
        val type = "Box<K, NonNullBox<V>>"

        // WHEN
        val result = eraseTypeParamsToAny(type, setOf("K", "V"), setOf("V"))

        // THEN
        assertEquals("Box<Any?, NonNullBox<*>>", result)
    }

    @Test
    fun `GIVEN type param name as prefix of another type WHEN eraseTypeParamsToAny THEN leaves other type intact`() {
        // GIVEN
        val type = "Box<T, Te>"

        // WHEN
        val result = eraseTypeParamsToAny(type, setOf("T"), setOf("T"))

        // THEN
        assertEquals("Box<*, Te>", result)
    }

    @Test
    fun `GIVEN bounded type param WHEN eraseTypeParamsSimple THEN star-projects type arguments`() {
        // GIVEN
        val type = "ABTest<T>"

        // WHEN
        val result = eraseTypeParamsSimple(type, setOf("T"), setOf("T"))

        // THEN
        assertEquals("ABTest<*>", result)
    }

    @Test
    fun `GIVEN type param declarations WHEN boundedTypeParamNames THEN returns only params with real bounds`() {
        // GIVEN
        val declarations =
            listOf("T : Enum<T>", "R", "out K : Any", "V : Any?", "in E : Comparable<E>")

        // WHEN
        val result = boundedTypeParamNames(declarations)

        // THEN
        assertEquals(setOf("T", "K", "E"), result)
    }
}
