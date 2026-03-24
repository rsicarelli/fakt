// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Unit tests for [applyGradleCompatibility].
 *
 * Tests apiVersion/languageVersion configuration via ProjectBuilder.
 * kotlin-stdlib exclusion (via `kotlin.stdlib.default.dependency=false`) is
 * verified by POM inspection after `publishToMavenLocal`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GradleCompatConventionTest {

    @Test
    fun `GIVEN gradle-plugin module WHEN applying gradle compatibility THEN apiVersion is KOTLIN_2_0`() {
        val project = ProjectBuilder.builder().withName("gradle-plugin").build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        project.applyGradleCompatibility()

        val ext = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        assertEquals(KotlinVersion.KOTLIN_2_0, ext.compilerOptions.apiVersion.orNull)
    }

    @Test
    fun `GIVEN gradle-plugin module WHEN applying gradle compatibility THEN languageVersion is KOTLIN_2_0`() {
        val project = ProjectBuilder.builder().withName("gradle-plugin").build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        project.applyGradleCompatibility()

        val ext = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        assertEquals(KotlinVersion.KOTLIN_2_0, ext.compilerOptions.languageVersion.orNull)
    }

    @Test
    fun `GIVEN compiler-api module WHEN applying gradle compatibility THEN apiVersion is KOTLIN_2_0`() {
        val project = ProjectBuilder.builder().withName("compiler-api").build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        project.applyGradleCompatibility()

        val ext = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        assertEquals(KotlinVersion.KOTLIN_2_0, ext.compilerOptions.apiVersion.orNull)
    }

    @Test
    fun `GIVEN compiler-api module WHEN applying gradle compatibility THEN languageVersion is KOTLIN_2_0`() {
        val project = ProjectBuilder.builder().withName("compiler-api").build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        project.applyGradleCompatibility()

        val ext = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        assertEquals(KotlinVersion.KOTLIN_2_0, ext.compilerOptions.languageVersion.orNull)
    }

    @Test
    fun `GIVEN compiler module WHEN applying gradle compatibility THEN apiVersion is not set`() {
        val project = ProjectBuilder.builder().withName("compiler").build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        project.applyGradleCompatibility()

        val ext = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        assertNull(ext.compilerOptions.apiVersion.orNull)
    }

    @Test
    fun `GIVEN compiler module WHEN applying gradle compatibility THEN languageVersion is not set`() {
        val project = ProjectBuilder.builder().withName("compiler").build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        project.applyGradleCompatibility()

        val ext = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        assertNull(ext.compilerOptions.languageVersion.orNull)
    }

    @Test
    fun `GIVEN annotations module WHEN applying gradle compatibility THEN apiVersion is not set`() {
        val project = ProjectBuilder.builder().withName("annotations").build()
        project.pluginManager.apply("org.jetbrains.kotlin.jvm")

        project.applyGradleCompatibility()

        val ext = project.extensions.getByType(KotlinJvmProjectExtension::class.java)
        assertNull(ext.compilerOptions.apiVersion.orNull)
    }
}
