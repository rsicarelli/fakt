// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for Fakt Gradle plugin.
 */
class FaktGradleSubpluginTest {
    @Test
    fun `plugin applies successfully to project`() {
        // Given: A test project
        val project = ProjectBuilder.builder().build()

        // When: Applying the Fakt plugin
        project.pluginManager.apply("com.rsicarelli.fakt")

        // Then: Plugin should be applied successfully
        assertTrue(project.plugins.hasPlugin("com.rsicarelli.fakt"))
    }

    @Test
    fun `plugin creates fakt extension`() {
        // Given: A project with Fakt plugin applied
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.rsicarelli.fakt")

        // When: Accessing the fakt extension
        val extension = project.extensions.findByName("fakt")

        // Then: Extension should exist and be the correct type
        assertNotNull(extension)
        assertTrue(extension is FaktPluginExtension)
    }

    @Test
    fun `extension has correct default values`() {
        // Given: A project with Fakt plugin applied
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.rsicarelli.fakt")

        // When: Accessing the extension
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        // Then: Extension should have expected defaults
        assertTrue(extension.enabled.get())
        assertFalse(extension.debug.get())
    }
}
