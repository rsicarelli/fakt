// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyJvmToolchain
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Convention plugin for Fakt sample modules.
 *
 * Applies:
 * - kotlin-multiplatform plugin
 * - All runtime targets (jvm, js, native, wasm)
 * - JVM toolchain (Java 21)
 * - KLIB duplicate name handling
 * - Common test dependencies
 */
class FaktSamplePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply plugins
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")

            // Apply JVM toolchain
            applyJvmToolchain()

            // Configure Kotlin Multiplatform using Kotlin DSL extension accessor
            the<KotlinMultiplatformExtension>().apply {
                applyDefaultHierarchyTemplate()

                // All targets matching runtime
                @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
                run {
                    jvm()
                    js {
                        browser()
                        nodejs()
                    }
                    iosX64()
                    iosArm64()
                    iosSimulatorArm64()
                    macosX64()
                    macosArm64()
                    linuxX64()
                    linuxArm64()
                    mingwX64()
                    wasmJs {
                        nodejs()
                    }
                }

                // Configure source sets
                sourceSets.apply {
                    getByName("commonMain") {
                        dependencies {
                            implementation("com.rsicarelli.fakt:runtime:1.0.0-SNAPSHOT")
                        }
                    }

                    getByName("commonTest") {
                        dependencies {
                            implementation("org.jetbrains.kotlin:kotlin-test:2.2.20")
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
                        }
                    }
                }

                // Fix KLIB duplicate unique_name error (centralized!)
                targets.all {
                    compilations.all {
                        compileTaskProvider.configure {
                            compilerOptions {
                                freeCompilerArgs.add("-Xklib-duplicated-unique-name-strategy=allow-first-with-warning")
                            }
                        }
                    }
                }
            }
        }
    }
}
