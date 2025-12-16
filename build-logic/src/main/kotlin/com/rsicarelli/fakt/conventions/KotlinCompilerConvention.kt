// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.conventions

import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

/**
 * Kotlin compiler convention.
 *
 * Configures:
 * - Progressive mode (catch deprecations early)
 * - Compiler flags:
 *   - JVM-specific: strict null safety, context parameters, etc.
 *   - Multiplatform-specific: context parameters only
 *
 * Note: JVM target is configured separately by applyJvmCompilation()
 */
fun Project.applyKotlinCompiler() {
    plugins.withType<KotlinBasePlugin> {
        tasks.withType<KotlinCompilationTask<*>>().configureEach {
            compilerOptions {
                // Enable progressive mode for all Kotlin compilations
                progressiveMode.set(true)

                // JVM-specific configuration
                if (this is KotlinJvmCompilerOptions) {
                    // Note: jvmTarget is set by applyJvmCompilation()

                    // JVM-specific compiler flags
                    freeCompilerArgs.addAll(
                        "-Xjsr305=strict", // Strict JSR-305 null safety
                        "-Xjvm-default=all", // Generate default methods for interfaces
                        "-Xtype-enhancement-improvements-strict-mode", // Strict type enhancements
                        "-Xcontext-parameters", // Enable context parameters
                    )
                } else {
                    // Multiplatform-specific compiler flags
                    freeCompilerArgs.addAll(
                        "-Xcontext-parameters", // Enable context parameters
                    )
                }
            }
        }
    }
}
