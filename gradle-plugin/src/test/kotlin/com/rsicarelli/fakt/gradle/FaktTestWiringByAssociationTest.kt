// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.createJvmProject
import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Pins that generated fakes reach a test compilation by its association with the main compilation
 * the producer belongs to, not by its name or by when it was created. The consumer is registered
 * first and the test compilation is created and associated in later statements, the order KGP
 * itself produces for Android variants and for `create(name) { associateWith(...) }`.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktTestWiringByAssociationTest {

    private fun Project.faktExtension(): FaktPluginExtension =
        extensions.getByType(FaktPluginExtension::class.java)

    /** KMP project with jvm + linuxX64 whose jvm consumer is registered before anything else. */
    private fun kmpWithConsumer(): Pair<Project, KotlinTarget> {
        val project = createKmpProject()
        val kotlin = project.getKotlinExtension()
        val jvm = kotlin.jvm()
        kotlin.linuxX64()
        FaktGenerateTaskWiring.registerConsumer(
            project,
            jvm.compilations.getByName("main"),
            project.faktExtension(),
        )
        return project to jvm
    }

    private fun Project.srcDirsOf(sourceSet: String): Set<File> =
        getKotlinExtension().sourceSets.getByName(sourceSet).kotlin.srcDirs

    private fun Project.dependenciesOfSourceSet(sourceSet: String): List<String> =
        getKotlinExtension()
            .sourceSets
            .getByName(sourceSet)
            .kotlin
            .buildDependencies
            .getDependencies(null)
            .map { it.name }

    private fun Project.dependenciesOfTask(taskName: String): List<String> =
        tasks.getByName(taskName).taskDependencies.getDependencies(null).map { it.name }

    // ---- B4 / B5: KMP ----------------------------------------------------------------------

    @Test
    fun `GIVEN consumer registered first WHEN a compilation is created then associated later THEN its source set and compile task are wired`() {
        val (project, jvm) = kmpWithConsumer()
        val main = jvm.compilations.getByName("main")
        val integration = jvm.compilations.create("integrationTest")

        integration.associateWith(main)

        val srcDirs = project.srcDirsOf("jvmIntegrationTest")
        assertTrue(
            srcDirs.any { it.path.endsWith("generated/fakt/jvm/main/kotlin") },
            "srcDirs: $srcDirs",
        )
        assertTrue("faktGenerateJvmMain" in project.dependenciesOfSourceSet("jvmIntegrationTest"))
        assertTrue(
            "faktGenerateJvmMain" in project.dependenciesOfTask(integration.compileKotlinTaskName),
            "the compile task must wait for the generator",
        )
    }

    @Test
    fun `GIVEN consumer registered first WHEN a compilation is created but never associated THEN nothing is wired into it or into main`() {
        val (project, jvm) = kmpWithConsumer()
        val loose = jvm.compilations.create("looseTest")

        val srcDirs = project.srcDirsOf("jvmLooseTest")
        assertTrue(
            srcDirs.none { it.path.contains("generated/fakt/jvm/main") },
            "srcDirs: $srcDirs",
        )
        assertFalse(
            "faktGenerateJvmMain" in project.dependenciesOfTask(loose.compileKotlinTaskName)
        )
        assertFalse(
            "faktGenerateJvmMain" in
                project.dependenciesOfTask(
                    jvm.compilations.getByName("main").compileKotlinTaskName
                ),
            "the main compile must never depend on its own test wiring",
        )
    }

    // ---- B6: non-KMP -----------------------------------------------------------------------

    @Test
    fun `GIVEN JVM producer WHEN a compilation without test in its name is associated later THEN its compile task is wired and main is not`(
        @TempDir tempDir: File
    ) {
        val project = createJvmProject(tempDir)
        val generatedDir =
            File(project.layout.buildDirectory.get().asFile, "generated/fakt/jvm/main/kotlin")
        generatedDir.mkdirs()
        val marker = File(generatedDir, "Marker.kt").apply { writeText("// marker\n") }
        val target = project.extensions.getByType(KotlinJvmProjectExtension::class.java).target
        val main = target.compilations.getByName("main")
        FaktGenerateTaskWiring.registerProducer(project, main, project.faktExtension())
        val integration = target.compilations.create("integration")

        integration.associateWith(main)

        val integrationCompile =
            project.tasks.getByName("compileIntegrationKotlin") as AbstractKotlinCompile<*>
        val testCompile = project.tasks.getByName("compileTestKotlin") as AbstractKotlinCompile<*>
        val mainCompile = project.tasks.getByName("compileKotlin") as AbstractKotlinCompile<*>
        assertTrue(
            marker in integrationCompile.sources.files,
            "${integrationCompile.sources.files}",
        )
        assertTrue("faktGenerateJvmMain" in project.dependenciesOfTask("compileIntegrationKotlin"))
        assertTrue(marker in testCompile.sources.files, "compileTestKotlin must stay wired")
        assertFalse(marker in mainCompile.sources.files, "compileKotlin must not be wired")
    }
}
