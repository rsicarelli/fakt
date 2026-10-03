// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle

import com.rsicarelli.fakt.gradle.helpers.createJvmProject
import com.rsicarelli.fakt.gradle.helpers.createKmpProject
import com.rsicarelli.fakt.gradle.helpers.evaluate
import com.rsicarelli.fakt.gradle.helpers.getKotlinExtension
import com.rsicarelli.fakt.gradle.helpers.jvmCompilation
import com.rsicarelli.fakt.gradle.helpers.kmpCompilation
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir

/**
 * Pins [configureCompilerOptions]: the compilation's compiler options and toolchain JDK reach the
 * `FaktGenerateTask` inputs, lazily, and without a dependency on `compileKotlin*`. Real
 * compilations from `ProjectBuilder` + KGP, no mocks.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FaktGenerateCompilerOptionsWiringTest {

    private fun jvmProject(dir: File): Project = createJvmProject(dir).also { it.evaluate() }

    /**
     * KMP with jvm + js so the metadata `commonMain`, `jvmMain` and `jsMain` compilations exist.
     */
    private fun kmpProject(): Project =
        createKmpProject().also {
            it.getKotlinExtension().jvm()
            it.getKotlinExtension().js { nodejs() }
            it.evaluate()
        }

    private fun Project.extension(): FaktPluginExtension =
        extensions.getByType(FaktPluginExtension::class.java)

    private fun Project.producer(compilation: KotlinCompilation<*>): FaktGenerateTask {
        FaktGenerateTaskWiring.registerProducer(this, compilation, extension())
        return tasks.withType(FaktGenerateTask::class.java).single()
    }

    private fun Project.kmpTask(target: String, compilation: String): FaktGenerateTask {
        val c = kmpCompilation(target, compilation)
        if (c.defaultSourceSet.name == "commonMain") {
            FaktGenerateTaskWiring.registerProducer(this, c, extension())
        } else {
            FaktGenerateTaskWiring.registerConsumer(this, c, extension())
        }
        return tasks.getByName(
            "faktGenerate" +
                target.replaceFirstChar { it.uppercase() } +
                compilation.replaceFirstChar { it.uppercase() }
        ) as FaktGenerateTask
    }

    @Test
    fun `GIVEN JVM compilerOptions optIn WHEN register THEN args contain opt-in`(
        @TempDir dir: File
    ) {
        val project = jvmProject(dir)
        project.compilationOptions().optIn.add("x.Marker")

        val args = project.producer(project.jvmCompilation("main")).compilerArguments.get()

        assertTrue("-opt-in=x.Marker" in args, "args: $args")
    }

    @Test
    fun `GIVEN KMP commonMain languageSettings WHEN register THEN producer args carry opt-in and feature`() {
        val project = kmpProject()
        val common = project.getKotlinExtension().sourceSets.getByName("commonMain")
        common.languageSettings.optIn("x.Marker")
        common.languageSettings.enableFeatureByReflection("ContextSensitiveResolution")

        val args = project.kmpTask("metadata", "commonMain").compilerArguments.get()

        assertTrue("-opt-in=x.Marker" in args, "args: $args")
        assertTrue("-XXLanguage:+ContextSensitiveResolution" in args, "args: $args")
    }

    @Test
    fun `GIVEN languageVersion and apiVersion on the compile task WHEN register THEN args carry them`(
        @TempDir dir: File
    ) {
        val project = jvmProject(dir)
        project.compilationOptions().languageVersion.set(KotlinVersion.KOTLIN_2_2)
        project.compilationOptions().apiVersion.set(KotlinVersion.KOTLIN_2_1)

        val args = project.producer(project.jvmCompilation("main")).compilerArguments.get()

        assertTrue(listOf("-language-version", "2.2") in args.windowed(2), "args: $args")
        assertTrue(listOf("-api-version", "2.1") in args.windowed(2), "args: $args")
    }

    @Test
    fun `GIVEN KMP jvm and js WHEN register THEN only the JVM task gets jvm-target`() {
        val project = kmpProject()

        val jvm = project.kmpTask("jvm", "main").compilerArguments.get()
        val common = project.kmpTask("metadata", "commonMain").compilerArguments.get()
        val js = project.kmpTask("js", "main").compilerArguments.get()

        assertTrue("-jvm-target" in jvm, "jvm args: $jvm")
        assertFalse("-jvm-target" in common, "common args: $common")
        assertFalse("-jvm-target" in js, "js args: $js")
    }

    @Test
    fun `GIVEN absolute path in freeCompilerArgs WHEN register THEN the entry is dropped`(
        @TempDir dir: File
    ) {
        val project = jvmProject(dir)
        project
            .compilationOptions()
            .freeCompilerArgs
            .addAll("-Xcontext-parameters", "-Xfoo=/abs/path")

        val args = project.producer(project.jvmCompilation("main")).compilerArguments.get()

        assertTrue("-Xcontext-parameters" in args, "args: $args")
        assertFalse(args.any { it.contains("/abs/path") }, "args: $args")
    }

    @Test
    fun `GIVEN optIn changed after register WHEN arguments read THEN the new value shows`(
        @TempDir dir: File
    ) {
        val project = jvmProject(dir)
        val task = project.producer(project.jvmCompilation("main"))

        project.compilationOptions().optIn.add("late.Marker")

        assertTrue("-opt-in=late.Marker" in task.compilerArguments.get())
    }

    @Test
    fun `GIVEN toolchain 21 WHEN register THEN JVM task has jdkVersion and jdkHome and js task has neither`() {
        val project = kmpProject()
        project.extensions
            .getByType(JavaPluginExtension::class.java)
            .toolchain
            .languageVersion
            .set(JavaLanguageVersion.of(21))

        val jvm = project.kmpTask("jvm", "main")
        val js = project.kmpTask("js", "main")

        assertEquals(21, jvm.jdkVersion.get())
        assertTrue(jvm.jdkHome.isPresent, "jdkHome must be set on the JVM task")
        assertNotNull(jvm.jdkHome.get().asFile)
        assertFalse(js.jdkVersion.isPresent)
        assertFalse(js.jdkHome.isPresent)
        assertNull(js.jdkVersion.orNull)
    }

    @Test
    fun `GIVEN JVM producer WHEN register THEN task depends on no compileKotlin task`(
        @TempDir dir: File
    ) {
        val project = jvmProject(dir)
        project.compilationOptions().optIn.add("x.Marker")

        val task = project.producer(project.jvmCompilation("main"))
        val deps = task.taskDependencies.getDependencies(task).map { it.name }

        assertTrue(deps.none { it.startsWith("compileKotlin") }, "dependencies: $deps")
        assertTrue(task.compilerArguments.get().isNotEmpty())
    }

    @Test
    fun `GIVEN each platform type WHEN asking for the toolchain JDK THEN only jvm uses it`() {
        val expected =
            mapOf(
                KotlinPlatformType.jvm to true,
                KotlinPlatformType.androidJvm to false,
                KotlinPlatformType.js to false,
                KotlinPlatformType.wasm to false,
                KotlinPlatformType.common to false,
                KotlinPlatformType.native to false,
            )

        val actual = expected.keys.associateWith(::usesToolchainJdk)

        assertEquals(expected, actual)
    }

    /**
     * `enableLanguageFeature` is deprecated at ERROR level in KGP 2.4 ("internal Kotlin compiler
     * argument") but still populates `enabledLanguageFeatures`, which is what users of older builds
     * rely on. Called reflectively so the test compiles without a suppression.
     */
    private fun LanguageSettingsBuilder.enableFeatureByReflection(name: String) {
        javaClass.getMethod("enableLanguageFeature", String::class.java).invoke(this, name)
    }

    private fun Project.compilationOptions() =
        jvmCompilation("main").compileTaskProvider.get().compilerOptions
}
