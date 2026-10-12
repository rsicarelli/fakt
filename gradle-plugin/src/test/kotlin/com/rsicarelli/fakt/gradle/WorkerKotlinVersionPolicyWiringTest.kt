// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.api.GradleException
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Wires the pure policy to Gradle: the injected-version seam, the Project wrapper, the call site.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkerKotlinVersionPolicyWiringTest {

    private val worker = "2.4.10"

    @Test
    fun `GIVEN user two minors newer WHEN checking THEN GradleException names both versions`() {
        val warnings = mutableListOf<String>()

        val failure =
            assertFailsWith<GradleException> {
                checkWorkerKotlinVersion("2.6.0", worker, allowNewer = false, warn = warnings::add)
            }

        val message = failure.message.orEmpty()
        assertTrue("2.6.0" in message && worker in message, message)
        assertTrue(warnings.isEmpty(), "A failure must not also warn.")
    }

    @Test
    fun `GIVEN user one minor newer WHEN checking THEN it warns once and does not throw`() {
        val warnings = mutableListOf<String>()

        checkWorkerKotlinVersion("2.5.0", worker, allowNewer = false, warn = warnings::add)

        assertEquals(1, warnings.size)
        assertTrue("2.5.0" in warnings.single() && worker in warnings.single())
    }

    @Test
    fun `GIVEN allowNewer WHEN user is two minors newer THEN it warns instead of throwing`() {
        val warnings = mutableListOf<String>()

        checkWorkerKotlinVersion("2.6.0", worker, allowNewer = true, warn = warnings::add)

        assertEquals(1, warnings.size)
        assertTrue("fakt.allowNewerKotlin" in warnings.single())
    }

    @Test
    fun `GIVEN supported or unknown user version WHEN checking THEN nothing is reported`() {
        val warnings = mutableListOf<String>()

        checkWorkerKotlinVersion("2.4.21", worker, allowNewer = false, warn = warnings::add)
        checkWorkerKotlinVersion("garbage", worker, allowNewer = false, warn = warnings::add)
        checkWorkerKotlinVersion(null, worker, allowNewer = false, warn = warnings::add)

        assertTrue(warnings.isEmpty())
    }

    @Test
    fun `GIVEN real KGP equal to the worker WHEN checking a project THEN no throw and no warning`(
        @TempDir tempDir: java.io.File
    ) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")
        val warnings = mutableListOf<String>()

        checkWorkerKotlinVersion(project, project.getKotlinPluginVersion(), warn = warnings::add)

        assertTrue(warnings.isEmpty(), warnings.toString())
    }

    @Test
    fun `GIVEN real KGP and a much older worker WHEN checking a project THEN it throws`(
        @TempDir tempDir: java.io.File
    ) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        assertFailsWith<GradleException> {
            checkWorkerKotlinVersion(project, workerKotlin = "1.0.0")
        }
    }

    @Test
    fun `GIVEN two projects of one build WHEN each claims the warning THEN only the first gets it`() {
        val root = ProjectBuilder.builder().build()
        val child = ProjectBuilder.builder().withParent(root).build()

        val first = workerVersionWarningOnce(root).claim()
        val second = workerVersionWarningOnce(child).claim()

        assertTrue(first)
        assertFalse(second)
    }

    @Test
    fun `GIVEN Fakt applied to a KMP project WHEN evaluating THEN the call site stays silent`(
        @TempDir tempDir: java.io.File
    ) {
        val project = ProjectBuilder.builder().withProjectDir(tempDir).build()
        project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        project.pluginManager.apply("com.rsicarelli.fakt")

        (project as ProjectInternal).evaluate()
    }
}
