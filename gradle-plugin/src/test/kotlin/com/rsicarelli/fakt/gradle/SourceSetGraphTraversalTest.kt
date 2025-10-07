// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD tests for graph traversal of KotlinSourceSet dependsOn relationships.
 * These tests define the expected BFS behavior BEFORE implementation.
 */
class SourceSetGraphTraversalTest {
    @Test
    fun `GIVEN single source set WHEN traversing THEN should return only itself`() {
        // GIVEN
        val commonMain = MockKotlinSourceSet(mockName = "commonMain")

        // WHEN
        val result = SourceSetGraphTraversal.getAllParentSourceSets(commonMain)

        // THEN
        assertEquals(1, result.size)
        assertTrue(result.contains(commonMain))
    }

    @Test
    fun `GIVEN simple hierarchy WHEN traversing THEN should find all parents`() {
        // GIVEN: jvmMain → commonMain
        val commonMain = MockKotlinSourceSet(mockName = "commonMain")
        val jvmMain =
            MockKotlinSourceSet(
                mockName = "jvmMain",
                mockDependsOn = setOf(commonMain),
            )

        // WHEN
        val result = SourceSetGraphTraversal.getAllParentSourceSets(jvmMain)

        // THEN
        assertEquals(2, result.size, "Should include jvmMain and commonMain")
        assertTrue(result.contains(jvmMain))
        assertTrue(result.contains(commonMain))
    }

    @Test
    fun `GIVEN deep hierarchy WHEN traversing THEN should find all levels`() {
        // GIVEN: iosX64Main → iosMain → appleMain → nativeMain → commonMain
        val commonMain = MockKotlinSourceSet(mockName = "commonMain")
        val nativeMain = MockKotlinSourceSet(mockName = "nativeMain", mockDependsOn = setOf(commonMain))
        val appleMain = MockKotlinSourceSet(mockName = "appleMain", mockDependsOn = setOf(nativeMain))
        val iosMain = MockKotlinSourceSet(mockName = "iosMain", mockDependsOn = setOf(appleMain))
        val iosX64Main = MockKotlinSourceSet(mockName = "iosX64Main", mockDependsOn = setOf(iosMain))

        // WHEN
        val result = SourceSetGraphTraversal.getAllParentSourceSets(iosX64Main)

        // THEN
        assertEquals(5, result.size, "Should include all 5 levels")
        assertTrue(result.contains(iosX64Main))
        assertTrue(result.contains(iosMain))
        assertTrue(result.contains(appleMain))
        assertTrue(result.contains(nativeMain))
        assertTrue(result.contains(commonMain))
    }

    @Test
    fun `GIVEN diamond dependency WHEN traversing THEN should not duplicate common parent`() {
        // GIVEN: Diamond pattern
        //        commonMain
        //        /        \
        //   nativeMain   appleMain
        //        \        /
        //         iosMain
        val commonMain = MockKotlinSourceSet(mockName = "commonMain")
        val nativeMain = MockKotlinSourceSet(mockName = "nativeMain", mockDependsOn = setOf(commonMain))
        val appleMain = MockKotlinSourceSet(mockName = "appleMain", mockDependsOn = setOf(commonMain))
        val iosMain =
            MockKotlinSourceSet(
                mockName = "iosMain",
                mockDependsOn = setOf(nativeMain, appleMain),
            )

        // WHEN
        val result = SourceSetGraphTraversal.getAllParentSourceSets(iosMain)

        // THEN
        assertEquals(4, result.size, "Should not duplicate commonMain")
        assertTrue(result.contains(iosMain))
        assertTrue(result.contains(nativeMain))
        assertTrue(result.contains(appleMain))
        assertTrue(result.contains(commonMain))

        // Verify commonMain appears exactly once
        val commonMainCount = result.count { it.name == "commonMain" }
        assertEquals(1, commonMainCount, "commonMain should appear exactly once")
    }

    @Test
    fun `GIVEN multiple direct parents WHEN traversing THEN should include all branches`() {
        // GIVEN: Custom hierarchy with multiple parents
        //   commonMain    utilsMain
        //        \        /
        //         jvmMain
        val commonMain = MockKotlinSourceSet(mockName = "commonMain")
        val utilsMain = MockKotlinSourceSet(mockName = "utilsMain")
        val jvmMain =
            MockKotlinSourceSet(
                mockName = "jvmMain",
                mockDependsOn = setOf(commonMain, utilsMain),
            )

        // WHEN
        val result = SourceSetGraphTraversal.getAllParentSourceSets(jvmMain)

        // THEN
        assertEquals(3, result.size)
        assertTrue(result.contains(jvmMain))
        assertTrue(result.contains(commonMain))
        assertTrue(result.contains(utilsMain))
    }

    @Test
    fun `GIVEN hierarchy map builder WHEN building THEN should create correct structure`() {
        // GIVEN
        val commonMain = MockKotlinSourceSet(mockName = "commonMain")
        val jvmMain = MockKotlinSourceSet(mockName = "jvmMain", mockDependsOn = setOf(commonMain))

        // WHEN
        val map = SourceSetGraphTraversal.buildHierarchyMap(jvmMain)

        // THEN
        assertEquals(2, map.size)
        assertEquals(listOf("commonMain"), map["jvmMain"])
        assertEquals(emptyList(), map["commonMain"])
    }

    @Test
    fun `GIVEN complex hierarchy map WHEN building THEN should preserve parent relationships`() {
        // GIVEN: iOS hierarchy
        val commonMain = MockKotlinSourceSet(mockName = "commonMain")
        val nativeMain = MockKotlinSourceSet(mockName = "nativeMain", mockDependsOn = setOf(commonMain))
        val appleMain = MockKotlinSourceSet(mockName = "appleMain", mockDependsOn = setOf(nativeMain))
        val iosMain = MockKotlinSourceSet(mockName = "iosMain", mockDependsOn = setOf(appleMain))

        // WHEN
        val map = SourceSetGraphTraversal.buildHierarchyMap(iosMain)

        // THEN
        assertEquals(4, map.size)
        assertEquals(listOf("appleMain"), map["iosMain"])
        assertEquals(listOf("nativeMain"), map["appleMain"])
        assertEquals(listOf("commonMain"), map["nativeMain"])
        assertEquals(emptyList(), map["commonMain"])
    }

    @Test
    fun `GIVEN diamond hierarchy map WHEN building THEN should list all direct parents`() {
        // GIVEN: Diamond pattern
        val commonMain = MockKotlinSourceSet(mockName = "commonMain")
        val nativeMain = MockKotlinSourceSet(mockName = "nativeMain", mockDependsOn = setOf(commonMain))
        val appleMain = MockKotlinSourceSet(mockName = "appleMain", mockDependsOn = setOf(commonMain))
        val iosMain =
            MockKotlinSourceSet(
                mockName = "iosMain",
                mockDependsOn = setOf(nativeMain, appleMain),
            )

        // WHEN
        val map = SourceSetGraphTraversal.buildHierarchyMap(iosMain)

        // THEN
        assertEquals(4, map.size)

        // iosMain should list BOTH direct parents (sorted)
        val iosMainParents = map["iosMain"]!!.sorted()
        assertEquals(listOf("appleMain", "nativeMain"), iosMainParents)

        // Both intermediate nodes point to commonMain
        assertEquals(listOf("commonMain"), map["nativeMain"])
        assertEquals(listOf("commonMain"), map["appleMain"])
    }
}

/**
 * Mock implementation of KotlinSourceSet for testing.
 * Simulates the dependsOn relationship graph without requiring full Gradle setup.
 *
 * Only implements the properties/methods needed for graph traversal tests.
 * All other methods throw errors to catch misuse.
 */
@Suppress("DEPRECATION")
private class MockKotlinSourceSet(
    private val mockName: String,
    private val mockDependsOn: Set<KotlinSourceSet> = emptySet(),
) : KotlinSourceSet {
    override fun getName(): String = mockName

    override val dependsOn: Set<KotlinSourceSet> = mockDependsOn

    override fun dependsOn(other: KotlinSourceSet) {
        error("Not supported in mock - set via constructor")
    }

    // Required properties from interfaces - all unused in graph traversal
    override val kotlin: org.gradle.api.file.SourceDirectorySet
        get() = error("Not used in graph traversal tests")

    override fun kotlin(configure: org.gradle.api.file.SourceDirectorySet.() -> Unit) = error("Not used in graph traversal tests")

    override fun kotlin(configure: org.gradle.api.Action<org.gradle.api.file.SourceDirectorySet>) =
        error("Not used in graph traversal tests")

    override val resources: org.gradle.api.file.SourceDirectorySet
        get() = error("Not used in graph traversal tests")

    override val languageSettings: org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
        get() = error("Not used in graph traversal tests")

    override fun languageSettings(configure: org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder.() -> Unit) =
        error("Not used in graph traversal tests")

    override fun languageSettings(configure: org.gradle.api.Action<org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder>) =
        error("Not used in graph traversal tests")

    override fun dependencies(configure: org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler.() -> Unit) {
        error("Not used in graph traversal tests")
    }

    override fun dependencies(configure: org.gradle.api.Action<org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler>) {
        error("Not used in graph traversal tests")
    }

    override val customSourceFilesExtensions: Iterable<String>
        get() = error("Not used in graph traversal tests")

    // From HasProject
    override val project: org.gradle.api.Project
        get() = error("Not used in graph traversal tests")

    // From HasMutableExtras
    override val extras: org.jetbrains.kotlin.tooling.core.MutableExtras
        get() = error("Not used in graph traversal tests")

    // From HasKotlinDependencies
    override val apiConfigurationName: String
        get() = error("Not used in graph traversal tests")

    override val compileOnlyConfigurationName: String
        get() = error("Not used in graph traversal tests")

    override val implementationConfigurationName: String
        get() = error("Not used in graph traversal tests")

    override val runtimeOnlyConfigurationName: String
        get() = error("Not used in graph traversal tests")

    // Deprecated properties
    override val apiMetadataConfigurationName: String
        get() = error("Deprecated - not used")

    override val implementationMetadataConfigurationName: String
        get() = error("Deprecated - not used")

    override val compileOnlyMetadataConfigurationName: String
        get() = error("Deprecated - not used")

    override val runtimeOnlyMetadataConfigurationName: String
        get() = error("Deprecated - not used")

    override fun toString(): String = "MockKotlinSourceSet(name=$mockName)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MockKotlinSourceSet) return false
        return mockName == other.mockName
    }

    override fun hashCode(): Int = mockName.hashCode()
}
