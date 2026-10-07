// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.worker

import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OutputRoutesTest {

    private val generated = File("build/generated/main").absoluteFile
    private val common = File("build/generated/common").absoluteFile

    @Test
    fun `GIVEN a generated token WHEN resolving THEN it maps to the generated directory`() {
        val routes =
            resolveOutputRoutes(mapOf("jvmMain" to "fakt://generated"), generated, common = null)

        assertEquals(mapOf("jvmMain" to generated.absolutePath), routes)
    }

    @Test
    fun `GIVEN a common token and a common directory WHEN resolving THEN it maps to the common directory`() {
        val routes = resolveOutputRoutes(mapOf("commonMain" to "fakt://common"), generated, common)

        assertEquals(mapOf("commonMain" to common.absolutePath), routes)
    }

    @Test
    fun `GIVEN both tokens WHEN resolving THEN each source set gets its own directory`() {
        val routes =
            resolveOutputRoutes(
                mapOf("jvmMain" to "fakt://generated", "commonMain" to "fakt://common"),
                generated,
                common,
            )

        assertEquals(
            mapOf("jvmMain" to generated.absolutePath, "commonMain" to common.absolutePath),
            routes,
        )
    }

    @Test
    fun `GIVEN an empty map WHEN resolving THEN it stays empty`() {
        val routes = resolveOutputRoutes(emptyMap(), generated, common)

        assertEquals(emptyMap(), routes)
    }

    @Test
    fun `GIVEN an unknown token WHEN resolving THEN it fails naming the token and the source set`() {
        val failure =
            assertFailsWith<IllegalStateException> {
                resolveOutputRoutes(mapOf("jvmMain" to "fakt://elsewhere"), generated, common)
            }

        assertContains(failure.message.orEmpty(), "fakt://elsewhere")
        assertContains(failure.message.orEmpty(), "jvmMain")
    }

    @Test
    fun `GIVEN a common token without a common directory WHEN resolving THEN it fails naming the token and the source set`() {
        val failure =
            assertFailsWith<IllegalStateException> {
                resolveOutputRoutes(
                    mapOf("commonMain" to "fakt://common"),
                    generated,
                    common = null,
                )
            }

        assertContains(failure.message.orEmpty(), "fakt://common")
        assertContains(failure.message.orEmpty(), "commonMain")
        assertContains(failure.message.orEmpty(), "commonGeneratedKotlinDir")
    }
}
