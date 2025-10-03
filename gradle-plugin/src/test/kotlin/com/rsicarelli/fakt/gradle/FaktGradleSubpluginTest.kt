// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for KtFakes Gradle plugin.
 */
class FaktGradleSubpluginTest {
    @Test
    fun `plugin applies successfully to project`() {
        // Given: A test project
        val project = ProjectBuilder.builder().build()

        // When: Applying the KtFakes plugin
        project.pluginManager.apply("com.rsicarelli.fakt")

        // Then: Plugin should be applied successfully
        assertTrue(project.plugins.hasPlugin("com.rsicarelli.fakt"))
    }

    @Test
    fun `plugin creates ktfake extension`() {
        // Given: A project with KtFakes plugin applied
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.rsicarelli.fakt")

        // When: Accessing the ktfake extension
        val extension = project.extensions.findByName("ktfake")

        // Then: Extension should exist and be the correct type
        assertNotNull(extension)
        assertTrue(extension is FaktPluginExtension)
    }

    @Test
    fun `extension has correct default values`() {
        // Given: A project with KtFakes plugin applied
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.rsicarelli.fakt")

        // When: Accessing the extension
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        // Then: Extension should have expected defaults
        assertTrue(extension.enabled.get())
        assertTrue(extension.generateCallTracking.get())
        assertTrue(extension.generateBuilderPatterns.get())
        assertTrue(extension.threadSafetyChecks.get())
    }
}
