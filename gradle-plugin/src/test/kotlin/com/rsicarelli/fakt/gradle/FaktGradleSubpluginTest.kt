// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test

/** Unit tests for Fakt Gradle plugin. */
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
    }

    // ==========================================
    // enableCallHistory Tests (Call History Control)
    // ==========================================

    @Test
    fun `GIVEN extension WHEN accessing enableCallHistory THEN has default true`() {
        // Given: A project with Fakt plugin applied
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.rsicarelli.fakt")

        // When: Accessing the extension
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        // Then: enableCallHistory should default to true
        assertTrue(extension.enableCallHistory.get(), "enableCallHistory should default to true")
    }

    @Test
    fun `GIVEN extension WHEN setting enableCallHistory to false THEN value is false`() {
        // Given: A project with Fakt plugin applied
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.rsicarelli.fakt")
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        // When: Setting enableCallHistory to false
        extension.enableCallHistory.set(false)

        // Then: enableCallHistory should be false
        assertFalse(
            extension.enableCallHistory.get(),
            "enableCallHistory should be false after being set",
        )
    }

    @Test
    fun `GIVEN extension WHEN setting enableCallHistory to true THEN value is true`() {
        // Given: A project with Fakt plugin applied
        val project = ProjectBuilder.builder().build()
        project.pluginManager.apply("com.rsicarelli.fakt")
        val extension = project.extensions.getByType(FaktPluginExtension::class.java)

        // When: Setting enableCallHistory to true explicitly
        extension.enableCallHistory.set(true)

        // Then: enableCallHistory should be true
        assertTrue(
            extension.enableCallHistory.get(),
            "enableCallHistory should be true after being set",
        )
    }
}
