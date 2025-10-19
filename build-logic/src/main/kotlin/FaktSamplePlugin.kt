// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyJvmToolchain
import com.rsicarelli.fakt.conventions.applyTestConventions
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

            // Apply test conventions (JUnit Platform, etc.)
            applyTestConventions()

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

                // NOTE: Dependencies are declared manually in each module's build.gradle.kts
                // for clarity when examining the sample project structure.
                // See individual modules for commonMain and commonTest dependencies.

                // Fix KLIB duplicate unique_name error for KLIB targets (Native, JS, Wasm)
                // JVM targets don't use KLIB format, so this flag doesn't apply
                targets.matching {
                    it.platformType in setOf(
                        org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.native,
                        org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.js,
                        org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType.wasm
                    )
                }.all {
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
