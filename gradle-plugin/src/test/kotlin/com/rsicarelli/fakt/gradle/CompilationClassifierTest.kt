// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

package com.rsicarelli.fakt.gradle

import org.gradle.api.Action
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * TDD tests for compilation classification (test vs main).
 * These tests define the expected heuristics BEFORE implementation.
 *
 * **Heuristics**:
 * 1. Standard test compilation name = "test"
 * 2. Convention: name ends with "Test"
 * 3. Associated with main compilation (test suite pattern)
 */
class CompilationClassifierTest {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    @Test
    fun `GIVEN standard test compilation WHEN classifying THEN should return true`() {
        // GIVEN
        val compilation =
            FakeKotlinCompilation(
                name = KotlinCompilation.TEST_COMPILATION_NAME, // "test"
            )

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertTrue(result, "Standard 'test' compilation should be classified as test")
    }

    @Test
    fun `GIVEN main compilation WHEN classifying THEN should return false`() {
        // GIVEN
        val compilation =
            FakeKotlinCompilation(
                name = KotlinCompilation.MAIN_COMPILATION_NAME, // "main"
            )

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertFalse(result, "Main compilation should not be classified as test")
    }

    @Test
    fun `GIVEN integrationTest compilation WHEN classifying THEN should return true`() {
        // GIVEN: Custom test suite following convention
        val compilation = FakeKotlinCompilation(name = "integrationTest")

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertTrue(
            result,
            "Custom 'integrationTest' should be classified as test (ends with 'Test')",
        )
    }

    @Test
    fun `GIVEN e2eTest compilation WHEN classifying THEN should return true`() {
        // GIVEN
        val compilation = FakeKotlinCompilation(name = "e2eTest")

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertTrue(result, "e2eTest should be classified as test")
    }

    @Test
    fun `GIVEN performanceTest compilation WHEN classifying THEN should return true`() {
        // GIVEN
        val compilation = FakeKotlinCompilation(name = "performanceTest")

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertTrue(result, "performanceTest should be classified as test")
    }

    @Test
    fun `GIVEN benchmark compilation WHEN classifying THEN should return false`() {
        // GIVEN: Not following Test suffix convention
        val compilation = FakeKotlinCompilation(name = "benchmark")

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertFalse(result, "benchmark (without Test suffix) should not be classified as test")
    }

    @Test
    fun `GIVEN compilation associated with main WHEN classifying THEN should return true`() {
        // GIVEN: Custom compilation associated with main (test suite pattern)
        val main = FakeKotlinCompilation(name = "main")
        val custom =
            FakeKotlinCompilation(
                name = "customSuite",
                associatedWith = setOf(main),
            )

        // WHEN
        val result = custom.isTestCompilation

        // THEN
        assertTrue(
            result,
            "Compilation associated with main should be classified as test (test suite pattern)",
        )
    }

    @Test
    fun `GIVEN compilation not associated with main WHEN classifying THEN should check name`() {
        // GIVEN: Custom compilation NOT associated with main
        val compilation =
            FakeKotlinCompilation(
                name = "custom",
                associatedWith = emptySet(),
            )

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertFalse(
            result,
            "Custom compilation without Test suffix should not be classified as test",
        )
    }

    @Test
    fun `GIVEN androidUnitTest WHEN classifying THEN should return true`() {
        // GIVEN: Android unit test variant
        val compilation = FakeKotlinCompilation(name = "androidUnitTest")

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertTrue(result, "androidUnitTest should be classified as test")
    }

    @Test
    fun `GIVEN debugTest WHEN classifying THEN should return true`() {
        // GIVEN: Android debug test variant
        val compilation = FakeKotlinCompilation(name = "debugTest")

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertTrue(result, "debugTest (Android variant) should be classified as test")
    }

    @Test
    fun `GIVEN releaseTest WHEN classifying THEN should return true`() {
        // GIVEN: Android release test variant
        val compilation = FakeKotlinCompilation(name = "releaseTest")

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertTrue(result, "releaseTest (Android variant) should be classified as test")
    }

    @Test
    fun `GIVEN debug compilation WHEN classifying THEN should return false`() {
        // GIVEN: Android debug variant (NOT test)
        val compilation = FakeKotlinCompilation(name = "debug")

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertFalse(result, "debug (Android variant) should not be classified as test")
    }

    @Test
    fun `GIVEN release compilation WHEN classifying THEN should return false`() {
        // GIVEN: Android release variant (NOT test)
        val compilation = FakeKotlinCompilation(name = "release")

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertFalse(result, "release (Android variant) should not be classified as test")
    }

    @Test
    fun `GIVEN case insensitive Test suffix WHEN classifying THEN should return true`() {
        // GIVEN: Compilation with "test" in lowercase
        val compilation = FakeKotlinCompilation(name = "integrationtest")

        // WHEN
        val result = compilation.isTestCompilation

        // THEN
        assertTrue(result, "Should handle case-insensitive 'test' suffix")
    }
}

/**
 * Minimal fake implementation for testing.
 * Only implements the properties that CompilationClassifier actually uses.
 */
@Suppress("DEPRECATION_ERROR")
private class FakeKotlinCompilation(
    name: String,
    associatedWith: Set<KotlinCompilation<*>> = emptySet(),
) : KotlinCompilation<Any> {
    override fun getName(): String = compilationName

    override val compilationName: String = name
    override val allAssociatedCompilations: Set<KotlinCompilation<*>> = associatedWith

    // Everything else throws errors to catch misuse
    override val implementationConfigurationName get() = error("Not used")
    override val apiConfigurationName get() = error("Not used")
    override val compileOnlyConfigurationName get() = error("Not used")
    override val runtimeOnlyConfigurationName get() = error("Not used")
    override val associatedCompilations get() = error("Not used")
    override val target get() = error("Not used")
    override val kotlinSourceSets get() = error("Not used")
    override val allKotlinSourceSets get() = error("Not used")
    override val defaultSourceSet get() = error("Not used")
    override val extras get() = error("Not used")
    override val project get() = error("Not used")
    override val compileDependencyConfigurationName get() = error("Not used")
    override var compileDependencyFiles: FileCollection
        get() = error("Not used")
        set(_) = error("Not used")
    override val runtimeDependencyConfigurationName get() = error("Not used")
    override val runtimeDependencyFiles get() = error("Not used")
    override val output get() = error("Not used")
    override val compileKotlinTaskName get() = error("Not used")
    override val compileTaskProvider get() = error("Not used")
    override val compileAllTaskName get() = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override val compilerOptions get() = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override val compileKotlinTask get() = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override val compileKotlinTaskProvider get() = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override val kotlinOptions get() = error("Not used")

    override fun defaultSourceSet(configure: KotlinSourceSet.() -> Unit) = error("Not used")

    override fun defaultSourceSet(configure: Action<KotlinSourceSet>) = error("Not used")

    override fun getAttributes(): AttributeContainer = error("Not used")

    override fun attributes(configure: AttributeContainer.() -> Unit) = error("Not used")

    override fun attributes(configure: Action<AttributeContainer>) = error("Not used")

    override fun associateWith(other: KotlinCompilation<*>) = error("Not used")

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit) = error("Not used")

    override fun dependencies(configure: Action<KotlinDependencyHandler>) = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override fun source(sourceSet: KotlinSourceSet) = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override fun kotlinOptions(configure: org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions.() -> Unit) = error("Not used")

    @Suppress("OVERRIDE_DEPRECATION")
    override fun kotlinOptions(configure: Action<org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions>) = error("Not used")
}
