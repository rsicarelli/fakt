// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import com.rsicarelli.fakt.gradle.helpers.kmpCompilation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.TestInstance

/**
 * Tests for simplified source set configuration (no custom source sets).
 *
 * This validates that we work WITH KMP's default hierarchy template, not against it. Generated code
 * is added to EXISTING test source sets.
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
        assertTrue(
            commonTest.kotlin.srcDirs.any {
                it.path.contains("build/generated/fakt/commonTest/kotlin")
            }
        )
    }

    @Test
    fun `GIVEN common producer task exists WHEN configuring KMP test source set dirs THEN commonTest generated dir is built by the producer`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()
        kotlin.linuxX64()
        project.evaluate()
        FaktGenerateTaskWiring.registerProducer(
            project,
            project.kmpCompilation("metadata", "commonMain"),
            project.extensions.getByType(FaktPluginExtension::class.java),
        )

        // When
        SourceSetConfigurator(project).configureKmpTestSourceSetDirs()

        // Then - the generated commonTest srcDir declares its producing task as a build dependency
        // so AGP lintAnalyze*/lintReport* pass Gradle 9.6+ implicit-dependency validation (#129).
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val deps = commonTest.kotlin.buildDependencies.getDependencies(null).map { it.name }
        assertTrue(
            deps.contains("faktGenerateMetadataCommonMain"),
            "commonTest generated dir must declare its producing FaktGenerateTask as builtBy; deps: $deps",
        )
    }

    @Test
    fun `GIVEN no producer task WHEN configuring KMP test source set dirs THEN commonTest generated dir has no Fakt build dependency`() {
        // Given - legacy mode: a single-target Native project registers no FaktGenerateTask
        // (Native is not drivable yet, #152), so generation is in-process at compile time
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.linuxX64()
        project.evaluate()

        // When
        SourceSetConfigurator(project).configureKmpTestSourceSetDirs()

        // Then - the dir stays a plain-File registration (nothing to wire builtBy to)
        val commonTest = kotlin.sourceSets.getByName("commonTest")
        val deps = commonTest.kotlin.buildDependencies.getDependencies(null).map { it.name }
        assertTrue(
            deps.none { it.startsWith("faktGenerate") },
            "Without a producer task the generated dir stays plain-File (no builtBy); deps: $deps",
        )
    }

    @Test
    fun `GIVEN single-target JVM KMP WHEN configured THEN jvmTest reads the single-target task output`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()

        // When
        project.evaluate()

        // Then - issue #153: the lone jvm main owns its platform fakes from one task
        assertConsumerOwned(kotlin.sourceSets.getByName("jvmTest"), targetName = "jvm")
    }

    @Test
    fun `GIVEN single-target JS KMP WHEN configured THEN jsTest reads the single-target task output`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.js { nodejs() }

        // When
        project.evaluate()

        // Then
        assertConsumerOwned(kotlin.sourceSets.getByName("jsTest"), targetName = "js")
    }

    @Test
    fun `GIVEN single-target JVM KMP WHEN configured THEN commonTest reads the canonical dir built by the single-target task`() {
        // Given
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()

        // When
        project.evaluate()
        val commonTest = kotlin.sourceSets.getByName("commonTest")

        // Then - the task owns the canonical commonTest dir: registered once, with its builtBy
        val canonical =
            commonTest.kotlin.srcDirs.filter {
                it.invariantSeparatorsPath.endsWith("build/generated/fakt/commonTest/kotlin")
            }
        assertEquals(1, canonical.size, "commonTest srcDirs=${commonTest.kotlin.srcDirs}")
        val deps = commonTest.kotlin.buildDependencies.getDependencies(null).map { it.name }
        assertTrue(
            deps.contains("faktGenerateJvmMain"),
            "commonTest must depend on the task that fills it; deps: $deps",
        )
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
        assertTrue(
            iosTest.kotlin.srcDirs.any {
                it.path.contains("build/generated/fakt/iosArm64Test/kotlin")
            }
        )
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

        // Then - commonTest and the Native test set keep the canonical dir; jvmTest and jsTest are
        // fed by their consumer FaktGenerateTask's output instead (issue #151)
        assertTrue(
            commonTest.kotlin.srcDirs.any {
                it.path.contains("build/generated/fakt/commonTest/kotlin")
            }
        )
        assertConsumerOwned(jvmTest, targetName = "jvm")
        assertConsumerOwned(jsTest, targetName = "js")
        assertTrue(
            iosTest.kotlin.srcDirs.any {
                it.path.contains("build/generated/fakt/iosArm64Test/kotlin")
            }
        )
    }

    @Test
    fun `GIVEN KMP project with iOS WHEN configured THEN iosX64Test should include its own generated dir`() {
        // GIVEN
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        kotlin.jvm()
        kotlin.iosX64()

        // WHEN
        project.evaluate()
        val iosX64Test = kotlin.sourceSets.getByName("iosX64Test")

        // THEN - Should include ONLY iosX64Test dir (not commonTest)
        // PASS 2 was removed: KMP dependency propagation handles KLIB visibility
        assertTrue(
            iosX64Test.kotlin.srcDirs.any {
                it.path.contains("build/generated/fakt/iosX64Test/kotlin")
            },
            "iosX64Test should have its own generated directory",
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
    fun `GIVEN KMP project with all platforms WHEN configured THEN all platform tests should have own dirs`() {
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

        // THEN - Each Native platform test set has its own in-process generated dir (not
        // commonTest); jvmTest and jsTest are fed by their consumer FaktGenerateTask (issue #151).
        // PASS 2 was removed: KMP dependency propagation handles KLIB visibility
        val nativeTestSourceSets = listOf("iosX64Test", "iosArm64Test", "iosSimulatorArm64Test")
        assertConsumerOwned(kotlin.sourceSets.getByName("jvmTest"), targetName = "jvm")
        assertConsumerOwned(kotlin.sourceSets.getByName("jsTest"), targetName = "js")

        nativeTestSourceSets.forEach { sourceSetName ->
            val sourceSet = kotlin.sourceSets.getByName(sourceSetName)
            assertTrue(
                sourceSet.kotlin.srcDirs.any {
                    it.path.contains("build/generated/fakt/$sourceSetName/kotlin")
                },
                "$sourceSetName should have its own generated directory",
            )
        }
    }

    /**
     * A test source set whose platform main is driven by a consumer `FaktGenerateTask` reads the
     * task's `generated/fakt/<target>/main/kotlin` output and must NOT also carry the canonical
     * in-process dir, where a stale copy from an earlier build would compile as a `Redeclaration`.
     */
    private fun assertConsumerOwned(
        sourceSet: org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet,
        targetName: String,
    ) {
        val srcDirs = sourceSet.kotlin.srcDirs
        assertTrue(
            srcDirs.any { it.path.contains("build/generated/fakt/$targetName/main/kotlin") },
            "${sourceSet.name} must read its consumer task output; srcDirs=$srcDirs",
        )
        assertTrue(
            srcDirs.none { it.path.contains("build/generated/fakt/${sourceSet.name}/kotlin") },
            "${sourceSet.name} must not carry the in-process dir; srcDirs=$srcDirs",
        )
    }
}
