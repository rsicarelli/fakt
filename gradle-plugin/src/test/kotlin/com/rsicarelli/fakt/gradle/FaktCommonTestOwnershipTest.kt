// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import com.rsicarelli.fakt.gradle.helpers.kmpCompilation
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * The owner registry is the single rule for `commonTest`: the metadata `commonMain` producer, the
 * synthetic `commonMain` producer and the single-target task each claim it, and
 * `SourceSetConfigurator` then keeps its plain directory out, leaving the task output as the one
 * canonical `commonTest` srcDir (which carries the task dependency, the #129 lint contract).
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktCommonTestOwnershipTest {

    private fun Project.faktExtension(): FaktPluginExtension =
        extensions.getByType(FaktPluginExtension::class.java)

    /**
     * Every `commonTest` srcDir registration that resolves to the canonical task output directory.
     * The public views (`srcDirs`, `srcDirTrees`, `sourceDirectories`) are sets and hide a repeated
     * registration, so this reads the registrations Gradle keeps before de-duplication (the private
     * `source` list of `DefaultSourceDirectorySet`; no public API exposes it).
     */
    private fun Project.canonicalCommonTestDirs(): List<String> {
        val kotlin = getKotlinExtension().sourceSets.getByName("commonTest").kotlin
        return registeredSrcDirs(kotlin)
            .flatMap { files(it).files }
            .map { it.path }
            .filter { it.endsWith(CANONICAL_COMMON_TEST_DIR) }
    }

    private fun registeredSrcDirs(kotlin: SourceDirectorySet): List<*> {
        var type: Class<*>? = kotlin.javaClass
        while (type != null && type.declaredFields.none { it.name == "source" }) {
            type = type.superclass
        }
        val field = checkNotNull(type) { "no `source` registrations on ${kotlin.javaClass}" }
        return field.getDeclaredField("source").apply { isAccessible = true }.get(kotlin) as List<*>
    }

    private fun Project.commonTestDeps(): List<String> =
        getKotlinExtension()
            .sourceSets
            .getByName("commonTest")
            .kotlin
            .buildDependencies
            .getDependencies(null)
            .map { it.name }

    private fun metadataProducerProject(): Project =
        createKmpProject().also {
            it.getKotlinExtension().jvm()
            it.getKotlinExtension().linuxX64()
            it.evaluate()
            FaktGenerateTaskWiring.registerProducer(
                it,
                it.kmpCompilation("metadata", "commonMain"),
                it.faktExtension(),
            )
        }

    private fun syntheticProject(): Project =
        createKmpProject().also {
            it.getKotlinExtension().jvm("desktop")
            it.getKotlinExtension().jvm("server")
            it.evaluate()
        }

    private fun singleTargetProject(): Project =
        createKmpProject().also {
            it.getKotlinExtension().jvm()
            it.evaluate()
            FaktGenerateTaskWiring.registerSingleTarget(
                it,
                it.kmpCompilation("jvm", "main"),
                it.faktExtension(),
            )
        }

    @Test
    fun `GIVEN the metadata commonMain producer WHEN registered THEN the registry claims commonTest for it`() {
        val project = metadataProducerProject()

        assertEquals("faktGenerateMetadataCommonMain", testDirOwnerOf(project, "commonTest"))
        assertTrue(isFaktTestDirOwner(project, "faktGenerateMetadataCommonMain"))
    }

    @Test
    fun `GIVEN the synthetic producer WHEN registered THEN the registry claims commonTest for it`() {
        val project = syntheticProject()

        assertEquals("faktGenerateCommonMain", testDirOwnerOf(project, "commonTest"))
        assertTrue(isFaktTestDirOwner(project, "faktGenerateCommonMain"))
    }

    @Test
    fun `GIVEN a single-target task WHEN registered THEN the registry claims commonTest for it`() {
        val project = singleTargetProject()

        assertEquals("faktGenerateJvmMain", testDirOwnerOf(project, "commonTest"))
        assertTrue(isFaktTestDirOwner(project, "faktGenerateJvmMain"))
    }

    @Test
    fun `GIVEN the metadata producer WHEN the test dirs are configured THEN commonTest has one canonical dir carrying the task`() {
        val project = metadataProducerProject()

        SourceSetConfigurator(project).configureKmpTestSourceSetDirs()

        assertEquals(
            1,
            project.canonicalCommonTestDirs().size,
            "${project.canonicalCommonTestDirs()}",
        )
        assertTrue("faktGenerateMetadataCommonMain" in project.commonTestDeps())
    }

    @Test
    fun `GIVEN the single-target task WHEN the test dirs are configured THEN commonTest has one canonical dir carrying the task`() {
        val project = singleTargetProject()

        SourceSetConfigurator(project).configureKmpTestSourceSetDirs()

        assertEquals(
            1,
            project.canonicalCommonTestDirs().size,
            "${project.canonicalCommonTestDirs()}",
        )
        assertTrue("faktGenerateJvmMain" in project.commonTestDeps(), "${project.commonTestDeps()}")
    }

    @Test
    fun `GIVEN the synthetic producer WHEN the test dirs are configured THEN commonTest has one canonical dir carrying the task`() {
        val project = syntheticProject()

        SourceSetConfigurator(project).configureKmpTestSourceSetDirs()

        assertEquals(
            1,
            project.canonicalCommonTestDirs().size,
            "${project.canonicalCommonTestDirs()}",
        )
        assertTrue("faktGenerateCommonMain" in project.commonTestDeps())
    }
}
