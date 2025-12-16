// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.conventions

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Configures Java plugin extension if the Java plugin is applied.
 *
 * Example:
 * ```kotlin
 * javaPluginExtension {
 *     sourceCompatibility = JavaVersion.VERSION_11
 *     targetCompatibility = JavaVersion.VERSION_11
 * }
 * ```
 */
internal inline fun Project.javaPluginExtension(block: JavaPluginExtension.() -> Unit) =
    extensions.findByType(JavaPluginExtension::class.java)?.also(block)

/**
 * Configures Kotlin JVM plugin extension if the Kotlin JVM plugin is applied.
 *
 * Example:
 * ```kotlin
 * kotlinJvmExtension {
 *     compilerOptions {
 *         jvmTarget.set(JvmTarget.JVM_11)
 *     }
 * }
 * ```
 */
internal inline fun Project.kotlinJvmExtension(block: KotlinJvmProjectExtension.() -> Unit) =
    extensions.findByType(KotlinJvmProjectExtension::class.java)?.also(block)

/**
 * Configures Kotlin Multiplatform plugin extension if the Kotlin Multiplatform plugin is applied.
 *
 * Example:
 * ```kotlin
 * kotlinMultiplatformExtension {
 *     targets.configureEach {
 *         // Configure targets
 *     }
 * }
 * ```
 */
internal inline fun Project.kotlinMultiplatformExtension(block: KotlinMultiplatformExtension.() -> Unit) =
    extensions.findByType(KotlinMultiplatformExtension::class.java)?.also(block)
