// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.helpers

import java.io.File
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

/**
 * Creates a test project with Kotlin Multiplatform and Fakt plugins applied.
 *
 * @return Configured test project
 */
internal fun createKmpProject(): Project {
    val project = ProjectBuilder.builder().build()
    // Set default group and version for capability resolution
    project.group = "com.example"
    project.version = "1.0.0"
    project.pluginManager.apply("org.jetbrains.kotlin.multiplatform")
    project.pluginManager.apply("com.rsicarelli.fakt")
    return project
}

/**
 * Creates a test project with Kotlin JVM and Fakt plugins applied.
 *
 * @param projectDir Project directory, typically a JUnit `@TempDir`
 * @return Configured test project
 */
internal fun createJvmProject(projectDir: File): Project {
    val project = ProjectBuilder.builder().withProjectDir(projectDir).build()
    project.group = "com.example"
    project.version = "1.0.0"
    project.pluginManager.apply("org.jetbrains.kotlin.jvm")
    project.pluginManager.apply("com.rsicarelli.fakt")
    return project
}

/**
 * Returns a real [KotlinCompilation] from the single-platform JVM target.
 *
 * Call after [evaluate] so the Kotlin Gradle Plugin has populated the compilation container.
 *
 * @param name Compilation name, e.g. `main` or `test`
 */
internal fun Project.jvmCompilation(name: String): KotlinCompilation<*> =
    extensions.getByType(KotlinJvmProjectExtension::class.java).target.compilations.getByName(name)

/**
 * Returns a real [KotlinCompilation] from a KMP target.
 *
 * Call after [evaluate] so the Kotlin Gradle Plugin has populated the compilation containers. The
 * metadata target owns the `commonMain` compilation when the project has multiple targets.
 *
 * @param targetName Target name, e.g. `jvm`, `linuxX64`, or `metadata`
 * @param compilationName Compilation name, e.g. `main` or `commonMain`
 */
internal fun Project.kmpCompilation(
    targetName: String,
    compilationName: String,
): KotlinCompilation<*> =
    getKotlinExtension().targets.getByName(targetName).compilations.getByName(compilationName)

/**
 * Gets the Kotlin Multiplatform extension from the project.
 *
 * @return KotlinMultiplatformExtension instance
 * @throws IllegalStateException if extension not found
 */
internal fun Project.getKotlinExtension(): KotlinMultiplatformExtension =
    extensions.getByType(KotlinMultiplatformExtension::class.java)

/**
 * Evaluates the project to trigger afterEvaluate blocks.
 *
 * In Gradle TestKit, we need to manually trigger evaluation to run afterEvaluate blocks.
 */
internal fun Project.evaluate() {
    // Gradle's ProjectBuilder doesn't automatically run afterEvaluate blocks
    // We need to manually trigger them for testing
    (this as ProjectInternal).evaluate()
}
