// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.gradle.helpers

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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
 * Gets the Kotlin Multiplatform extension from the project.
 *
 * @return KotlinMultiplatformExtension instance
 * @throws IllegalStateException if extension not found
 */
internal fun Project.getKotlinExtension(): KotlinMultiplatformExtension = extensions.getByType(KotlinMultiplatformExtension::class.java)

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
