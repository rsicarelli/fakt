// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.android.build.gradle.LibraryExtension
import com.rsicarelli.fakt.conventions.applyJvmCompilation
import com.rsicarelli.fakt.conventions.applyTestConventions
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for Fakt Android sample modules.
 *
 * Applies:
 * - Android Library plugin (com.android.library)
 * - Kotlin Android plugin (org.jetbrains.kotlin.android)
 * - Android configuration (compileSdk 35, minSdk 24)
 * - Java 11 source/target compatibility
 * - Common test dependencies (JUnit 5, parallel execution)
 *
 * The Android `namespace` is derived from the module name so multi-module Android samples get a
 * unique R-class package per module (two modules sharing a namespace collide on `R`). The single
 * `android-single-module` sample keeps its historical namespace, since camel-casing its name yields
 * exactly `androidSingleModule`.
 */
class FaktSampleAndroidPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply Android Library and Kotlin Android plugins
            pluginManager.apply("com.android.library")
            pluginManager.apply("org.jetbrains.kotlin.android")

            // Configure Android Library extension
            extensions.configure<LibraryExtension> {
                namespace = "com.rsicarelli.fakt.samples.${moduleNamespaceSuffix(target.name)}"
                compileSdk = 35 // Latest stable

                defaultConfig {
                    minSdk = 24 // Android 7.0 (85%+ devices)
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
            }

            // Apply JVM compilation (Kotlin target)
            applyJvmCompilation()

            // Apply test conventions (JUnit Platform, etc.)
            applyTestConventions()
        }
    }
}

/** Converts a kebab-case module name to a camelCase namespace suffix (`a-b-c` -> `aBC`). */
private fun moduleNamespaceSuffix(moduleName: String): String =
    moduleName.split('-', '_').filter { it.isNotEmpty() }.mapIndexed { index, part ->
        if (index == 0) part else part.replaceFirstChar { it.uppercaseChar() }
    }.joinToString("")
