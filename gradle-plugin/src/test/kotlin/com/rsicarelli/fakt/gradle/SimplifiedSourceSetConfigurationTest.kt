// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for simplified source set configuration (no custom source sets).
 *
 * This validates that we work WITH KMP's default hierarchy template,
 * not against it. Generated code is added to EXISTING test source sets.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SimplifiedSourceSetConfigurationTest {
    @Test
    fun `GIVEN KMP project WHEN plugin applied THEN should NOT create fakes source set`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()

        // When
        project.evaluate()

        // Then - Should NOT create custom 'fakes' source set
        assertNull(kotlin.sourceSets.findByName("fakes"))
    }

    @Test
    fun `GIVEN KMP project with JVM WHEN plugin applied THEN should NOT create jvmFakes source set`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()

        // When
        project.evaluate()

        // Then - Should NOT create custom 'jvmFakes' source set
        assertNull(kotlin.sourceSets.findByName("jvmFakes"))
    }

    @Test
    fun `GIVEN KMP project with JS WHEN plugin applied THEN should NOT create jsFakes source set`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.js { nodejs() }

        // When
        project.evaluate()

        // Then - Should NOT create custom 'jsFakes' source set
        assertNull(kotlin.sourceSets.findByName("jsFakes"))
    }

    @Test
    fun `GIVEN KMP project with iOS WHEN plugin applied THEN should NOT create iosArm64Fakes source set`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.iosArm64()

        // When
        project.evaluate()

        // Then - Should NOT create custom 'iosArm64Fakes' source set
        assertNull(kotlin.sourceSets.findByName("iosArm64Fakes"))
    }

    @Test
    fun `GIVEN commonTest source set WHEN configured THEN should include build generated fakt fakes kotlin`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()

        // When
        project.evaluate()
        val commonTest = kotlin.sourceSets.getByName("commonTest")

        // Then - Should add generated dir to EXISTING commonTest
        assertTrue(commonTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/commonTest/kotlin") })
    }

    @Test
    fun `GIVEN jvmTest source set WHEN configured THEN should include build generated fakt jvmFakes kotlin`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()

        // When
        project.evaluate()
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")

        // Then - Should add generated dir to EXISTING jvmTest
        assertTrue(jvmTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/jvmTest/kotlin") })
    }

    @Test
    fun `GIVEN jsTest source set WHEN configured THEN should include build generated fakt jsFakes kotlin`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.js { nodejs() }

        // When
        project.evaluate()
        val jsTest = kotlin.sourceSets.getByName("jsTest")

        // Then - Should add generated dir to EXISTING jsTest
        assertTrue(jsTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/jsTest/kotlin") })
    }

    @Test
    fun `GIVEN iosArm64Test source set WHEN configured THEN should include build generated fakt iosArm64Fakes kotlin`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.iosArm64()

        // When
        project.evaluate()
        val iosTest = kotlin.sourceSets.getByName("iosArm64Test")

        // Then - Should add generated dir to EXISTING iosArm64Test
        assertTrue(iosTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/iosArm64Test/kotlin") })
    }

    @Test
    fun `GIVEN multiple targets WHEN configured THEN all test source sets should include generated dirs`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()
        kotlin.js { nodejs() }
        kotlin.iosArm64()

        // When
        project.evaluate()

        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val jvmTest = kotlin.sourceSets.getByName("jvmTest")
        val jsTest = kotlin.sourceSets.getByName("jsTest")
        val iosTest = kotlin.sourceSets.getByName("iosArm64Test")

        // Then - All should have their respective generated directories
        assertTrue(commonTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/commonTest/kotlin") })
        assertTrue(jvmTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/jvmTest/kotlin") })
        assertTrue(jsTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/jsTest/kotlin") })
        assertTrue(iosTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/iosArm64Test/kotlin") })
    }

    @Test
    fun `GIVEN KMP project with iOS WHEN configured THEN iosX64Test should include commonTest generated dir`() {
        // GIVEN
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()
        kotlin.iosX64()

        // WHEN
        project.evaluate()
        val iosX64Test = kotlin.sourceSets.getByName("iosX64Test")

        // THEN - Should include BOTH iosX64Test dir AND commonTest dir
        assertTrue(
            iosX64Test.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/iosX64Test/kotlin") },
            "iosX64Test should have its own generated directory",
        )
        assertTrue(
            iosX64Test.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/commonTest/kotlin") },
            "iosX64Test should also have access to commonTest generated directory",
        )
    }

    @Test
    fun `GIVEN KMP project WHEN configured THEN commonTest should NOT have duplicate commonTest dir`() {
        // GIVEN
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()

        // WHEN
        project.evaluate()
        val commonTest = kotlin.sourceSets.getByName("commonTest")

        // THEN - commonTest should have exactly ONE occurrence of its directory
        val commonTestDirs =
            commonTest.kotlin.srcDirs.filter {
                it.path.contains("build/generated/fakt/commonTest/kotlin")
            }
        assertEquals(
            1,
            commonTestDirs.size,
            "commonTest should have exactly one occurrence of its generated directory",
        )
    }

    @Test
    fun `GIVEN KMP project with all platforms WHEN configured THEN all platform tests should see commonTest`() {
        // GIVEN
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()
        kotlin.js { nodejs() }
        kotlin.iosX64()
        kotlin.iosArm64()
        kotlin.iosSimulatorArm64()

        // WHEN
        project.evaluate()

        // THEN - ALL platform test source sets should include commonTest dir
        val platformTestSourceSets =
            listOf(
                "jvmTest",
                "jsTest",
                "iosX64Test",
                "iosArm64Test",
                "iosSimulatorArm64Test",
            )

        platformTestSourceSets.forEach { sourceSetName ->
            val sourceSet = kotlin.sourceSets.getByName(sourceSetName)
            assertTrue(
                sourceSet.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/commonTest/kotlin") },
                "$sourceSetName should have access to commonTest generated directory",
            )
        }
    }
}
