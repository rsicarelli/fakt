// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import org.junit.jupiter.api.TestInstance
import kotlin.test.Test
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
        assertTrue(commonTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/fakes/kotlin") })
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
        assertTrue(jvmTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/jvmFakes/kotlin") })
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
        assertTrue(jsTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/jsFakes/kotlin") })
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
        assertTrue(iosTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/iosArm64Fakes/kotlin") })
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
        assertTrue(commonTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/fakes/kotlin") })
        assertTrue(jvmTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/jvmFakes/kotlin") })
        assertTrue(jsTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/jsFakes/kotlin") })
        assertTrue(iosTest.kotlin.srcDirs.any { it.path.contains("build/generated/fakt/iosArm64Fakes/kotlin") })
    }

    // ========================================
    // Compiler Plugin Output Directory
    // ========================================

    @Test
    fun `GIVEN project with commonTest WHEN compiling main THEN compiler should output to fakes kotlin`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()
        project.evaluate()

        val mainCompilation =
            kotlin.targets
                .getByName("jvm")
                .compilations
                .getByName("main")
        val configurator = SourceSetConfigurator(project)

        // When
        val outputDir = configurator.getGeneratedSourcesDirectory(mainCompilation)

        // Then - Should output to common directory (not jvmFakes) because commonTest exists
        assertTrue(outputDir.contains("build/generated/fakt/fakes/kotlin"))
    }
}
