// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.fakes.FakeKotlinSourceSet
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
        val commonMain = FakeKotlinSourceSet(name = "commonMain")

        // WHEN
        val result = SourceSetGraphTraversal.getAllParentSourceSets(commonMain)

        // THEN
        assertEquals(1, result.size)
        assertTrue(result.contains(commonMain))
    }

    @Test
    fun `GIVEN simple hierarchy WHEN traversing THEN should find all parents`() {
        // GIVEN: jvmMain → commonMain
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val jvmMain =
            FakeKotlinSourceSet(
                name = "jvmMain",
                parents = setOf(commonMain),
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
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val nativeMain = FakeKotlinSourceSet(name = "nativeMain", parents = setOf(commonMain))
        val appleMain = FakeKotlinSourceSet(name = "appleMain", parents = setOf(nativeMain))
        val iosMain = FakeKotlinSourceSet(name = "iosMain", parents = setOf(appleMain))
        val iosX64Main = FakeKotlinSourceSet(name = "iosX64Main", parents = setOf(iosMain))

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
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val nativeMain = FakeKotlinSourceSet(name = "nativeMain", parents = setOf(commonMain))
        val appleMain = FakeKotlinSourceSet(name = "appleMain", parents = setOf(commonMain))
        val iosMain =
            FakeKotlinSourceSet(
                name = "iosMain",
                parents = setOf(nativeMain, appleMain),
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
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val utilsMain = FakeKotlinSourceSet(name = "utilsMain")
        val jvmMain =
            FakeKotlinSourceSet(
                name = "jvmMain",
                parents = setOf(commonMain, utilsMain),
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
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val jvmMain = FakeKotlinSourceSet(name = "jvmMain", parents = setOf(commonMain))

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
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val nativeMain = FakeKotlinSourceSet(name = "nativeMain", parents = setOf(commonMain))
        val appleMain = FakeKotlinSourceSet(name = "appleMain", parents = setOf(nativeMain))
        val iosMain = FakeKotlinSourceSet(name = "iosMain", parents = setOf(appleMain))

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
        val commonMain = FakeKotlinSourceSet(name = "commonMain")
        val nativeMain = FakeKotlinSourceSet(name = "nativeMain", parents = setOf(commonMain))
        val appleMain = FakeKotlinSourceSet(name = "appleMain", parents = setOf(commonMain))
        val iosMain =
            FakeKotlinSourceSet(
                name = "iosMain",
                parents = setOf(nativeMain, appleMain),
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
