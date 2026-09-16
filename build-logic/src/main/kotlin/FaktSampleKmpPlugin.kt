// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0

import com.rsicarelli.fakt.conventions.applyJvmCompilation
import com.rsicarelli.fakt.conventions.applyTestConventions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootExtension

/**
 * Convention plugin for Fakt KMP sample modules.
 *
 * Applies:
 * - kotlin-multiplatform plugin
 * - All runtime targets (jvm, js, native, wasm)
 * - JVM compilation (explicit Java 11 target, no toolchains)
 * - KLIB duplicate name handling
 * - Common test dependencies
 */
class FaktSampleKmpPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            // Apply plugins
            pluginManager.apply("org.jetbrains.kotlin.multiplatform")

            // Apply JVM compilation (explicit target, no toolchains)
            applyJvmCompilation()

            // Apply test conventions (JUnit Platform, etc.)
            applyTestConventions()

            // Relocate the committed yarn.lock files to vendor/kotlin-js-store. GitHub's
            // Dependency Graph indexes kotlin-js-store/yarn.lock as an npm_and_yarn manifest, but
            // there's no sibling package.json for Dependabot to resolve (it's build output,
            // correctly gitignored) - so every matching security advisory spawns a Dependabot
            // Security Update job that fails deterministically. GitHub's dependency graph parser
            // skips manifests under vendor-style directory names, so move the lock files there.
            rootProject.plugins.withType(YarnPlugin::class.java) {
                rootProject.the<YarnRootExtension>().lockFileDirectory =
                    rootProject.rootDir.resolve("vendor/kotlin-js-store")
            }
            rootProject.plugins.withType(WasmYarnPlugin::class.java) {
                rootProject.the<WasmYarnRootExtension>().lockFileDirectory =
                    rootProject.rootDir.resolve("vendor/kotlin-js-store/wasm")
            }

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
