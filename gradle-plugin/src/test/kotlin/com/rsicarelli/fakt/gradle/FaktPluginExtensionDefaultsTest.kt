// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.compiler.api.LogLevel
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Pins the [FaktPluginExtension] property defaults that no other suite covers.
 *
 * `enabled` and `enableCallHistory` defaults live in [FaktGradleSubpluginTest]; the
 * `useExperimentalGenerateTask` default (`true` — the cache-correct path) lives in
 * [FaktExperimentalGenerateTaskWiringTest]. This suite covers the remaining conventions so a
 * default flip can never land silently.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktPluginExtensionDefaultsTest {

    private fun faktExtension(): FaktPluginExtension {
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.rsicarelli.fakt")
        return project.extensions.getByType(FaktPluginExtension::class.java)
    }

    @Test
    fun `GIVEN plugin applied WHEN reading logLevel THEN defaults to INFO`() {
        val extension = faktExtension()

        assertEquals(
            LogLevel.INFO,
            extension.logLevel.get(),
            "logLevel must default to INFO — concise summary without opting into DEBUG noise.",
        )
    }

    @Test
    fun `GIVEN plugin applied WHEN reading enableMutableFakes THEN defaults to false`() {
        val extension = faktExtension()

        assertFalse(
            extension.enableMutableFakes.get(),
            "enableMutableFakes must default to false — fakes are immutable unless opted in.",
        )
    }

    @Test
    fun `GIVEN plugin applied WHEN reading useGradleTestFixtures THEN defaults to false`() {
        val extension = faktExtension()

        assertFalse(
            extension.useGradleTestFixtures.get(),
            "useGradleTestFixtures must default to false — test source set is the default output.",
        )
    }

    @OptIn(ExperimentalFaktMultiModule::class)
    @Test
    fun `GIVEN plugin applied WHEN reading collectFrom THEN is not present`() {
        val extension = faktExtension()

        assertFalse(
            extension.collectFrom.isPresent,
            "collectFrom must be unset by default — generator mode is the default mode.",
        )
    }
}
