// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

package com.rsicarelli.fakt.conventions

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/** Target Java version for bytecode compatibility (Java 11 minimum) */
private val targetJavaVersion = JavaVersion.VERSION_11

/** Target JVM version for Kotlin compiler (JVM 11) */
private val targetJvmVersion = JvmTarget.JVM_11

/**
 * Applies JVM compilation configuration for all applicable plugins.
 *
 * Handles:
 * - Java plugin: sourceCompatibility/targetCompatibility
 * - Kotlin JVM plugin: modern compilerOptions API
 * - Kotlin Multiplatform plugin: per-target configuration for JVM-based targets
 * - Fallback: task-based configuration for edge cases
 */
internal fun Project.applyJvmCompilation() {
    // Configure Java plugin (if applied)
    javaPluginExtension {
        sourceCompatibility = targetJavaVersion
        targetCompatibility = targetJavaVersion
    }

    // Configure Kotlin JVM plugin (if applied)
    kotlinJvmExtension { compilerOptions { jvmTarget.set(targetJvmVersion) } }

    // Configure Kotlin Multiplatform plugin (if applied)
    kotlinMultiplatformExtension {
        targets.configureEach {
            // Only configure JVM-based targets (JVM, Android)
            if (!isJvmTarget()) return@configureEach

            compilations.configureEach {
                compileTaskProvider.configure {
                    compilerOptions {
                        // Type-safe check before setting JVM-specific options
                        if (this !is KotlinJvmCompilerOptions) return@compilerOptions
                        jvmTarget.set(targetJvmVersion)
                    }
                }
            }
        }
    }

    // Fallback: Configure all Kotlin compile tasks directly
    // This ensures jvmTarget is set even in edge cases
    tasks.withType(KotlinCompile::class.java).configureEach {
        compilerOptions { jvmTarget.set(targetJvmVersion) }
    }
}

/**
 * Detects if a Kotlin target is JVM-based (requires JVM target configuration).
 *
 * JVM-based targets include:
 * - `jvm` (standard JVM target)
 * - `androidJvm` (Android target)
 *
 * @return `true` if the target is JVM-based, `false` otherwise
 */
private fun KotlinTarget.isJvmTarget(): Boolean =
    when (platformType) {
        KotlinPlatformType.jvm,
        KotlinPlatformType.androidJvm,
        -> true
        else -> false
    }
